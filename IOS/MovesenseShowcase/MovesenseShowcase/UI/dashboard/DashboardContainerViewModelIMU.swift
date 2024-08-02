//
//  DashboardContainerViewModelIMU.swift
//  MovesenseShowcase
//
//  Created by Hapola, Tuomas on 4.12.2020.
//  Copyright © 2020 Suunto. All rights reserved.
//

import Foundation
import MovesenseApi

class DashboardContainerViewModelIMU: DashboardContainerViewModel {

    private var previousTimestamp: UInt32 = 0

    override func handleEvent(_ event: ObserverEvent) {
        guard let event = event as? MovesenseObserverEventOperation else { return }

        switch event {
        case .operationResponse: return
        case .operationFinished: return
        case .operationEvent(let event): self.receivedEvent(event)
        case .operationError(let error): self.onOperationError(error)
        }
    }

    func receivedEvent(_ event: MovesenseEvent) {
        guard case let MovesenseEvent.imu(_, imuData) = event,
              imuData.accVectors.count > 0 else {
            NSLog("DashboardContainerViewModelIMU::receivedEvent invalid event.")
            return
        }

        if previousTimestamp == 0 || previousTimestamp > imuData.timestamp {
            NSLog("DashboardContainerViewModelIMU::receivedEvent invalid timestamp, resetting.")
            previousTimestamp = imuData.timestamp
            return
        }

        let timeIncrement: Double = Double(imuData.timestamp - previousTimestamp) / Double(imuData.accVectors.count)
        imuData.accVectors.forEach { vector in
            // This requires serial observation queue to guarantee the order
            notifyObservers(DashboardObserverEventVector.receivedVector(x: vector.x, y: vector.y, z: vector.z,
                                                                        step: timeIncrement))
        }

        previousTimestamp = imuData.timestamp
    }

    func onOperationError(_ error: MovesenseError) {
        notifyObservers(DashboardObserverEventVector.onError("error: \(error.localizedDescription)\n"))
    }
}
