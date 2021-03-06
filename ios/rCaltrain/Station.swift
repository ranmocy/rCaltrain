//
//  Station.swift
//  rCaltrain
//
//  Created by Wanzhang Sheng on 10/25/14.
//  Copyright (c) 2014-2015 Ranmocy. All rights reserved.
//

import Foundation

class Station {

    // Class variables/methods
    fileprivate struct StationStruct {
        static var names = NSMutableOrderedSet()
        static var idToStation = [Int: Station]()
        static var nameToStations = [String: [Station]]()
    }

    class func getNames() -> [String] {
        return (StationStruct.names.array as! [String]).sorted(by: <)
    }
    class func getStation(byId id: Int) -> Station? {
        return StationStruct.idToStation[id]
    }
    class func getStations(byName name: String) -> [Station]? {
        return StationStruct.nameToStations[name]
    }
    class func addStation(name: String, id: Int) {
        let station = Station(name: name, id: id)

        StationStruct.names.add(name)
        StationStruct.idToStation[id] = station
        if (StationStruct.nameToStations[name] != nil) {
            StationStruct.nameToStations[name]!.append(station)
        } else {
            StationStruct.nameToStations[name] = [station]
        }
    }


    // Instance variables/methods
    let name: String
    let id: Int

    fileprivate init(name: String, id: Int) {
        self.name = name
        self.id = id
    }
}
