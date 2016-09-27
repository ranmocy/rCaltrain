//
//  Calendar.swift
//  rCaltrain
//
//  Created by Wanzhang Sheng on 2/19/15.
//  Copyright (c) 2015 Ranmocy. All rights reserved.
//

import Foundation

class Calendar {

    static let currentCalendar = Foundation.Calendar.current

    fileprivate let weekdays : [Bool]
    let start_date, end_date : Date

    init(dict: NSDictionary) {
        // service_id => {weekday: bool, saturday: bool, sunday: bool, start_date: date, end_date: date}
        // 4930 => {weekday: false, saturday: true, sunday: false, start_date: 20160404, end_date: 20190406}

        assert(dict.count == 5, "Expected item has 5 elements")

        // Make sunday as the first day, since we use Gregorian calendar
        var days = [dict.value(forKey: "sunday") as! Bool]
        let weekday = dict.value(forKey: "weekday") as! Bool
        for _ in 1...5 {
            days.append(weekday)
        }
        days.append(dict.value(forKey: "saturday") as! Bool)
        weekdays = days

        start_date = Date.parseDate(asYYYYMMDDInt: dict.value(forKey: "start_date") as! Int)
        end_date   = Date.parseDate(asYYYYMMDDInt: dict.value(forKey: "end_date") as! Int)
    }

    // day is in 1...7
    func isValid(weekday day : Int) -> Bool {
        return self.weekdays[day - 1]
    }

}

