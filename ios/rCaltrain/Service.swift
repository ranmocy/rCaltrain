//
//  Service.swift
//  rCaltrain
//
//  Created by Wanzhang Sheng on 10/25/14.
//  Copyright (c) 2014-2015 Ranmocy. All rights reserved.
//

import Foundation

class Service {

    // Class variables/methods
    fileprivate struct ServiceStruct {
        static var services = [Service]()
        static var idToServices = [String: [Service]]()
    }

    class func getAllServices() -> [Service] {
        return ServiceStruct.services
    }

    class func getServices(byId id: String) -> [Service]? {
        return ServiceStruct.idToServices[id]
    }

    class func addService(id: String, tripsDict : NSDictionary) -> Service {
        var trips = [String: Trip]()
        for (tripId, stopsArray) in tripsDict as! [String: NSArray] {
            trips[tripId] = Trip(id: tripId, stopsArray: stopsArray)
        }
        let service = Service(id: id, trips: trips)
        ServiceStruct.services.append(service)
        if (ServiceStruct.idToServices[id] != nil) {
            ServiceStruct.idToServices[id]!.append(service)
        } else {
            ServiceStruct.idToServices[id] = [service]
        }
        return service
    }


    // Instance variables/methods
    let id : String
    let trips : [String: Trip]
    var calendar : Calendar!
    var calendar_dates = [CalendarDates]()

    fileprivate init (id: String, trips: [String: Trip]) {
        self.id = id
        self.trips = trips
    }

    func isValid(atWeekday day: Int) -> Bool {
        let date = Date()
        return (calendar.start_date <= date) && (date <= calendar.end_date) && calendar.isValid(weekday: day)
    }

    func isValidAtToday() -> Bool {
        let date = Date()
        let day = Calendar.currentCalendar.dateComponents([.weekday], from: date).weekday

        var exceptional_add = false
        var exceptional_remove = false

        // Only Today will consider holiday
        // (inCalendar && not inDates2) || inDates1
        for eDate in calendar_dates {
            if (date.compare(eDate.exception_date as Date) == .orderedSame) {
                if (eDate.toAdd) {
                    exceptional_add = true
                } else {
                    exceptional_remove = true
                }
            }
        }

        return (isValid(atWeekday: day!) && !exceptional_remove) || exceptional_add
    }

    func isValidAtWeekday() -> Bool {
        // weekday is from 2 to 6, sunday is the first day
        for day in 2...6 {
            if (!isValid(atWeekday: day)) {
                return false
            }
        }
        return true
    }

    func isValidAtSaturday() -> Bool {
        return isValid(atWeekday: 7)
    }

    func isValidAtSunday() -> Bool {
        return isValid(atWeekday: 1)
    }

    func isValidAt(_ withCategory: String) -> Bool {
        switch withCategory {
        case "Now":
            return isValidAtToday()
        case "Weekday":
            return isValidAtWeekday()
        case "Saturday":
            return isValidAtSaturday()
        case "Sunday":
            return isValidAtSunday()
        default:
            return false
        }
    }

}
