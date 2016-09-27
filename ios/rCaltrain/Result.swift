//
//  Result.swift
//  rCaltrain
//
//  Created by Wanzhang Sheng on 2/17/15.
//  Copyright (c) 2015 Ranmocy. All rights reserved.
//

import Foundation

class Result {

    let departureStop: Stop
    let arrivalStop: Stop

    var departureTime: Date {
        return departureStop.departureTime as Date
    }
    var arrivalTime: Date {
        return arrivalStop.arrivalTime as Date
    }

    fileprivate func dateToStr(_ date: Date) -> String {
        let interval = Int(date.timeIntervalSince1970)
        let hours = String(interval / 3600 % 24).rjust(2, withStr: "0")
        let minutes = String(interval / 60 % 60).rjust(2, withStr: "0")
        return "\(hours):\(minutes)"
    }

    var departureStr: String {
        return dateToStr(departureTime)
    }
    var arrivalStr: String {
        return dateToStr(arrivalTime)
    }

    var duration: TimeInterval {
        return arrivalStop.arrivalTime.timeIntervalSince(departureStop.departureTime as Date)
    }
    var durationInMin: Int {
        return Int(duration) / 60
    }

    init(departure: Stop, arrival: Stop) {
        self.departureStop = departure
        self.arrivalStop = arrival
    }

}
