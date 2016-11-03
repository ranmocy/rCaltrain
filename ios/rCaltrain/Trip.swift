//
//  Trip.swift
//  rCaltrain
//
//  Created by Wanzhang Sheng on 10/23/14.
//  Copyright (c) 2014-2015 Ranmocy. All rights reserved.
//

import Foundation

class Trip {

    let id : String
    let stops : [Stop]

    init(id: String, stopsArray: NSArray) {
        var stops = [Stop]()
        for data in stopsArray as! [NSArray] {
            assert(data.count == 2, "data length is \(data.count), expected 2!")

            let stationId = data[0] as! Int;
            let time = Date(timeIntervalSince1970: TimeInterval(data[1] as! Int))
            
            if let station = Station.getStation(byId: stationId) {
                stops.append(Stop(station: station, departureTime: time, arrivalTime: time))
            } else {
                fatalError("can't find station id\(stationId)")
            }
        }
        self.id = id
        self.stops = stops
    }

    func findFrom(_ from: Station, to: Station) -> (Stop, Stop)? {
        var i: Int = 0
        var fromStop: Stop?, toStop: Stop?

        // find the departure stop
        while (i < stops.count){
            if (stops[i].station === from) {
                fromStop = stops[i]
                break
            }
            i += 1
        }

        // if missing
        if (fromStop == nil) {
            return nil
        }

        // from and to can't be the same
        i += 1

        // find the arrival stop
        while (i < stops.count) {
            if (stops[i].station === to) {
                toStop = stops[i]
                break
            }
            i += 1
        }

        // if missing
        if (toStop == nil) {
            return nil
        }
        
        return (fromStop!, toStop!)
    }

}
