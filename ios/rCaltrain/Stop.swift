//
//  Stop.swift
//  rCaltrain
//
//  Created by Wanzhang Sheng on 10/25/14.
//  Copyright (c) 2014-2015 Ranmocy. All rights reserved.
//

import Foundation

class Stop {

    let station: Station
    let departureTime: Date
    let arrivalTime: Date

    var laterThanNow: Bool {
        return departureTime > Date()
    }

    init(station: Station, departureTime dTime: Date, arrivalTime aTime: Date) {
        self.station = station
        self.departureTime = dTime
        self.arrivalTime = aTime
    }
    
}
