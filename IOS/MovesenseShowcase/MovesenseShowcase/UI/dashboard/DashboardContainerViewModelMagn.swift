//
// DashboardContainerViewModelMagn.swift
// MovesenseShowcase
//
// Copyright (c) 2019 Suunto. All rights reserved.
//

import Foundation
import MovesenseApi

class DashboardContainerViewModelMagn: DashboardContainerViewModel {

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
        guard case let MovesenseEvent.magn(_, magnData) = event,
              magnData.vectors.count > 0 else {
            NSLog("DashboardContainerViewModelMagn::receivedEvent invalid event.")
            return
        }

        if previousTimestamp == 0 || previousTimestamp > magnData.timestamp {
            NSLog("DashboardContainerViewModelMagn::receivedEvent invalid timestamp, resetting.")
            previousTimestamp = magnData.timestamp
            return
        }

        let timeIncrement: Double = Double(magnData.timestamp - previousTimestamp) / Double(magnData.vectors.count)
        magnData.vectors.forEach { vector in
            // This requires serial observation queue to guarantee the order
            notifyObservers(DashboardObserverEventVector.receivedVector(x: vector.x, y: vector.y, z: vector.z,
                                                                        step: timeIncrement))
        }

        previousTimestamp = magnData.timestamp
    }

    func onOperationError(_ error: MovesenseError) {
        notifyObservers(DashboardObserverEventVector.onError("error: \(error.localizedDescription)\n"))
    }
}
