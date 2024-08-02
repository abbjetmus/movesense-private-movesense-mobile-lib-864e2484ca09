//
// Recorder.swift
// MovesenseShowcase
//
// Copyright (c) 2018 Suunto. All rights reserved.
//

import Foundation
import MovesenseApi

enum RecorderObserverEvent: ObserverEvent {

    case idle
    case recording
    case recordsUpdated
    case recorderConverting(_ target: String, progress: Int)
    case recorderError(_ error: Error)
}

public struct RecorderApi {

    static let instance: Recorder = RecorderConcrete.sharedInstance
}

protocol Recorder: Observable {

    func startRecording()
    func stopRecording()

    func addDeviceOperation(_ device: MovesenseDevice, _ operation: MovesenseOperation)
    func removeDeviceOperation(_ device: MovesenseDevice, _ operation: MovesenseOperation)

    func removeAllOperations()

    func getRecords() -> [RecorderFile]
    func removeRecord(_ record: RecorderFile)

    func tempCopyRecord(_ record: RecorderFile) -> URL?
    func tempClear()

    func convertToCsv(_ recordUrl: URL) -> URL?
}

protocol RecorderDelegate: AnyObject {

    func recorderError(_ record: RecorderFileJson, _ error: Error)
}

class RecorderConcrete: Recorder {

    private enum Constants {
        static let accHeader: String = "timestamp,x,y,z"
        static let ecgHeader: String = "timestamp,sample"
        static let gyroHeader: String = "timestamp,x,y,z"
        static let magnHeader: String = "timestamp,x,y,z"
        static let imuHeader: String = "timestamp,x,y,z,gx,gy,gz"
        static let hrHeader: String = "average,rrData"
        static let lfData: Data = Data([UInt8(0x0a)]) // Linefeed UTF8 code point 0x0a
        static let commaData: Data = Data([UInt8(0x2c)]) // Comma UTF8 code point 0x2c
        static let lfUInt8: UInt8 = 0x0a
        static let commaUInt8: UInt8 = 0x2c
        static let semicolonUInt8: UInt8 = 0x3b
    }

    fileprivate static let sharedInstance: Recorder = RecorderConcrete()

    internal var observations: [Observation] = [Observation]()
    private(set) var observationQueue: DispatchQueue = DispatchQueue.global()

    private let jsonDecoder: JSONDecoder = JSONDecoder()

    private var conversionWorkItem: DispatchWorkItem?
    private var operationFiles: [RecorderFileJson] = []
    private var recordingDate: Date = Date()

    init() {
        jsonDecoder.dateDecodingStrategy = .iso8601
    }

    func startRecording() {
        recordingDate = Date()

        DispatchQueue.global().async { [recordingDate, operationFiles] in
            operationFiles.forEach {
                $0.delegate = self
                $0.startRecording(recordingDate)
            }
        }

        notifyObservers(RecorderObserverEvent.recording)
    }

    func stopRecording() {
        DispatchQueue.global().async { [weak self, recordingDate, operationFiles] in
            operationFiles.forEach { $0.stopRecording(recordingDate) }
            self?.notifyObservers(RecorderObserverEvent.idle)
        }
    }

    func addDeviceOperation(_ device: MovesenseDevice, _ operation: MovesenseOperation) {
        let newFile = RecorderFileJson(device: device, operation: operation)
        guard (operationFiles.contains { $0 == newFile } == false) else { return }
        operationFiles.append(newFile)
    }

    func removeDeviceOperation(_ device: MovesenseDevice, _ operation: MovesenseOperation) {
        operationFiles.removeAll { $0 == RecorderFileJson(device: device, operation: operation) }
    }

    func removeAllOperations() {
        operationFiles.removeAll()
    }

    func getRecords() -> [RecorderFile] {
        guard let storageUrl: URL = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first else {
            let error = AppError.operationError("Recorder::getRecords unable to get documents dir.")
            NSLog(error.localizedDescription)
            notifyObservers(RecorderObserverEvent.recorderError(error))
            return []
        }

        let recordPath: String = storageUrl.path + "/recordings/"
        guard let enumerator = FileManager.default.enumerator(atPath: recordPath) else {
            let error = AppError.operationError("Recorder::getRecords unable to read records.")
            NSLog(error.localizedDescription)
            notifyObservers(RecorderObserverEvent.recorderError(error))
            return []
        }

        let records: [RecorderFile] = enumerator.compactMap { $0 as? String }
            .filter { $0.hasSuffix(RecorderFileJson.Constants.headerSuffix) }
            .compactMap { FileManager.default.contents(atPath: recordPath + $0) }
            .compactMap { try? jsonDecoder.decode(RecorderFile.self, from: $0) }

        return records
    }

    func tempCopyRecord(_ record: RecorderFile) -> URL? {
        guard let storageUrl: URL = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first,
              let fileName = record.filePath.split(separator: "/").last else {
            return nil
        }

        let tempUrl = FileManager.default.temporaryDirectory
        let storageFilePath = storageUrl.path + record.filePath
        let fullFileName = record.startDate.iso8601 + "_" + record.serialNumber + "_" + fileName
        let tempFilePath = tempUrl.path + "/" + fullFileName

        // In case the file exists already
        try? FileManager.default.removeItem(atPath: tempFilePath)

        do {
            try FileManager.default.copyItem(atPath: storageFilePath, toPath: tempFilePath)
        } catch let error {
            notifyObservers(RecorderObserverEvent.recorderError(error))
            return nil
        }

        return URL(fileURLWithPath: tempFilePath)
    }

    func tempClear() {
        conversionWorkItem?.cancel()

        guard let filePaths = try? FileManager.default.contentsOfDirectory(at: FileManager.default.temporaryDirectory,
                                                                           includingPropertiesForKeys: nil,
                                                                           options: []) else {
            let error = AppError.operationError("Recorder::tempClear unable to get temporary dir contents.")
            notifyObservers(RecorderObserverEvent.recorderError(error))
            return
        }

        filePaths.forEach { filePath in try? FileManager.default.removeItem(at: filePath) }
    }

    func removeRecord(_ record: RecorderFile) {
        guard let storageUrl: URL = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first else {
            return
        }

        let recordPath: String = record.filePath.split(separator: "/").dropLast().joined(separator: "/")
        let documentsFilePath = storageUrl.path + "/" + recordPath
        do {
            try FileManager.default.removeItem(atPath: documentsFilePath)
        } catch let error {
            notifyObservers(RecorderObserverEvent.recorderError(error))
            return
        }

        // TODO: Remove directory as well if empty

        notifyObservers(RecorderObserverEvent.recordsUpdated)
    }

    func convertToCsv(_ tempUrl: URL) -> URL? {
        let csvFileUrl = tempUrl.deletingPathExtension().appendingPathExtension("csv")

        guard FileManager.default.createFile(atPath: csvFileUrl.path, contents: nil),
              let jsonHandle = try? FileHandle(forReadingFrom: tempUrl),
              let csvHandle = try? FileHandle(forWritingTo: csvFileUrl) else {
            return nil
        }

        let endOffset = jsonHandle.seekToEndOfFile()
        jsonHandle.seek(toFileOffset: 0)

        var relativeOffset: Int = 0
        let csvEncoder = CsvEncoder()

        guard let _ = jsonHandle.readLine(delimiter: "\n"),
              var lineData = jsonHandle.readLine(delimiter: "\n") else {
            //TODO: Generate error
            return nil
        }

        var secondLineOffset: UInt64 = 0
        if #available(iOS 13.4, *) {
            guard let offset = try? jsonHandle.offset() else {
                return nil
            }
            secondLineOffset = offset
        } else {
            secondLineOffset = jsonHandle.offsetInFile
        }

        // If the last character is comma, remove before decoding
        if Constants.commaData == lineData[lineData.count-2..<lineData.count-1] {
            lineData = lineData.dropLast(2)
        }

        guard let decodedEvent = try? jsonDecoder.decode(MovesenseEvent.self, from: lineData),
              let csvHeaderData = getCsvHeaderData(event: decodedEvent)
              else {
            //TODO: Generate error
            return nil
        }

        // Remove the first line even if converting from old files which are not proper json.
        jsonHandle.seek(toFileOffset: secondLineOffset)
        csvHandle.write(csvHeaderData.header + Constants.lfData)

        conversionWorkItem = DispatchWorkItem { [weak self] in

            // Calculate timestamps for array items
            var prevTimeStamp: UInt32 = 0

            // Convert JSON data line by line
            while self?.conversionWorkItem?.isCancelled == false {
                let doBreak = autoreleasepool { () -> Bool in
                guard var lineData = jsonHandle.readLine(delimiter: "\n") else {
                    return true
                }

                if Constants.commaData == lineData[lineData.count-2..<lineData.count-1] {
                    lineData = lineData.dropLast(2)
                }

                guard let decoded = try? self?.jsonDecoder.decode(MovesenseEvent.self, from: lineData) else {
                    //TODO: Generate error
                    return true
                }

                var eventArray: [MovesenseEvent] = []
                var newTimeStamp: UInt32 = 0

                switch decoded {
                case .acc:
                    guard let timeAndEvents = self?.getSplitAccData(event: decoded, prevTimeStamp: prevTimeStamp) else {
                        break
                    }
                    eventArray = timeAndEvents.events
                    newTimeStamp = timeAndEvents.timestamp
                case .ecg:
                    guard let timeAndEvents = self?.getSplitEcgData(event: decoded, prevTimeStamp: prevTimeStamp) else {
                        break
                    }
                    eventArray = timeAndEvents.events
                    newTimeStamp = timeAndEvents.timestamp
                case .gyroscope:
                    guard let timeAndEvents = self?.getSplitGyroData(event: decoded, prevTimeStamp: prevTimeStamp) else {
                        break
                    }
                    eventArray = timeAndEvents.events
                    newTimeStamp = timeAndEvents.timestamp
                case .magn:
                    guard let timeAndEvents = self?.getSplitMagnData(event: decoded, prevTimeStamp: prevTimeStamp) else {
                        break
                    }
                    eventArray = timeAndEvents.events
                    newTimeStamp = timeAndEvents.timestamp
                case .imu:
                    guard let timeAndEvents = self?.getSplitImuData(event: decoded, prevTimeStamp: prevTimeStamp) else {
                        break
                    }
                    eventArray = timeAndEvents.events
                    newTimeStamp = timeAndEvents.timestamp
                case .heartRate:
                    eventArray.append(decoded)
                    newTimeStamp = 1
                }


                let newRelativeOffset = Int(100 * Double(jsonHandle.offsetInFile) / Double(endOffset))
                if relativeOffset < newRelativeOffset {
                    relativeOffset = newRelativeOffset
                    self?.notifyObservers(RecorderObserverEvent.recorderConverting(csvFileUrl.lastPathComponent,
                                                                                   progress: relativeOffset))
                }


                if prevTimeStamp > 0 {
                    for it in eventArray {
                        guard let encoded = try? csvEncoder.encode(it) else {
                            //TODO: Generate error
                            break
                        }
                        csvHandle.write(encoded + Constants.lfData)
                    }
                }
                prevTimeStamp = newTimeStamp

                return false
            }

                if doBreak {
                    break
                }
            }
        }

        conversionWorkItem?.perform()

        if conversionWorkItem?.isCancelled == true {
            return nil
        }

        return csvFileUrl
    }


    private func getSplitAccData(event: MovesenseEvent, prevTimeStamp: UInt32) -> (timestamp: UInt32, events: [MovesenseEvent]) {
        guard case let MovesenseEvent.acc(r, arrayData) = event else {
            return (0, [])
        }
        var eventArray: [MovesenseEvent] = []
        let ts = arrayData.timestamp
        let deltaTs = Double(ts - prevTimeStamp)/Double(arrayData.vectors.count)
        for i in 0..<arrayData.vectors.count {

            let vecs : [MovesenseVector3D] = [arrayData.vectors[i]]
            let newEvent = MovesenseAcc(timestamp: ts + UInt32(Double(i)*deltaTs), vectors: vecs)
            eventArray.append(MovesenseEvent.acc(r, newEvent) as MovesenseEvent)
        }

        return (ts, eventArray)
    }

    private func getSplitGyroData(event: MovesenseEvent, prevTimeStamp: UInt32) -> (timestamp: UInt32, events: [MovesenseEvent]) {
        guard case let MovesenseEvent.gyroscope(r, arrayData) = event else {
            return (0, [])
        }
        var eventArray: [MovesenseEvent] = []
        let ts = arrayData.timestamp
        let deltaTs = Double(ts - prevTimeStamp)/Double(arrayData.vectors.count)
        for i in 0..<arrayData.vectors.count {

            let vecs : [MovesenseVector3D] = [arrayData.vectors[i]]
            let newEvent = MovesenseGyro(timestamp: ts + UInt32(Double(i)*deltaTs), vectors: vecs)
            eventArray.append(MovesenseEvent.gyroscope(r, newEvent) as MovesenseEvent)
        }

        return (ts, eventArray)
    }

    private func getSplitMagnData(event: MovesenseEvent, prevTimeStamp: UInt32) -> (timestamp: UInt32, events: [MovesenseEvent]) {
        guard case let MovesenseEvent.magn(r, arrayData) = event else {
            return (0, [])
        }
        var eventArray: [MovesenseEvent] = []
        let ts = arrayData.timestamp
        let deltaTs = Double(ts - prevTimeStamp)/Double(arrayData.vectors.count)
        for i in 0..<arrayData.vectors.count {

            let vecs : [MovesenseVector3D] = [arrayData.vectors[i]]
            let newEvent = MovesenseMagn(timestamp: ts + UInt32(Double(i)*deltaTs), vectors: vecs)
            eventArray.append(MovesenseEvent.magn(r, newEvent) as MovesenseEvent)
        }

        return (ts, eventArray)
    }

    private func getSplitImuData(event: MovesenseEvent, prevTimeStamp: UInt32) -> (timestamp: UInt32, events: [MovesenseEvent]) {
        guard case let MovesenseEvent.imu(r, arrayData) = event else {
            return (0, [])
        }
        var eventArray: [MovesenseEvent] = []
        let ts = arrayData.timestamp
        let deltaTs = Double(ts - prevTimeStamp)/Double(arrayData.accVectors.count)
        for i in 0..<arrayData.accVectors.count {

            let vecs1 : [MovesenseVector3D] = [arrayData.accVectors[i]]
            let vecs2 : [MovesenseVector3D] = [arrayData.gyroVectors[i]]
            let newEvent = MovesenseIMU(timestamp: ts + UInt32(Double(i)*deltaTs), accVectors: vecs1, gyroVectors: vecs2)
            eventArray.append(MovesenseEvent.imu(r, newEvent) as MovesenseEvent)
        }

        return (ts, eventArray)
    }

    private func getSplitEcgData(event: MovesenseEvent, prevTimeStamp: UInt32) -> (timestamp: UInt32, events: [MovesenseEvent]) {
        guard case let MovesenseEvent.ecg(r, arrayData) = event else {
            return (0, [])
        }
        var eventArray: [MovesenseEvent] = []
        let ts = arrayData.timestamp
        let deltaTs = Double(ts - prevTimeStamp)/Double(arrayData.samples.count)
        for i in 0..<arrayData.samples.count {

            let vecs : [Int32] = [arrayData.samples[i]]
            let newEvent = MovesenseEcg(timestamp: ts + UInt32(Double(i)*deltaTs), samples: vecs)
            eventArray.append(MovesenseEvent.ecg(r, newEvent) as MovesenseEvent)
        }

        return (ts, eventArray)
    }


    private func getCsvHeaderData(event: MovesenseEvent) -> (header: Data, numOfTimedMeas: UInt8)? {
        let csvHeaderString: String
        let numOfTimestamped: UInt8
        switch event {
        case .acc: csvHeaderString = Constants.accHeader; numOfTimestamped = 1
        case .ecg: csvHeaderString = Constants.ecgHeader; numOfTimestamped = 1
        case .gyroscope: csvHeaderString = Constants.gyroHeader; numOfTimestamped = 1
        case .magn: csvHeaderString = Constants.magnHeader; numOfTimestamped = 1
        case .imu: csvHeaderString = Constants.imuHeader; numOfTimestamped = 2
        case .heartRate: csvHeaderString = Constants.hrHeader; numOfTimestamped = 0
        }

        if let data = csvHeaderString.data(using: .utf8) {
            return (data, numOfTimestamped)
        } else {
            return nil
        }
    }
}

extension RecorderConcrete: RecorderDelegate {

    func recorderError(_ record: RecorderFileJson, _ error: Error) {
        stopRecording()
        notifyObservers(RecorderObserverEvent.recorderError(error))
    }
}
