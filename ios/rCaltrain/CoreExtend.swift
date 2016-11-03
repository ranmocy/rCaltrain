//
//  CoreExtend.swift
//  rCaltrain
//
//  Created by Wanzhang Sheng on 10/26/14.
//  Copyright (c) 2014-2015 Ranmocy. All rights reserved.
//

import Foundation

extension String {
    var length: Int {
        get {
            return self.characters.count
        }
    }

    func `repeat`(_ times: Int) -> String {
        var str = ""
        for _ in 0..<times {
            str += self
        }
        return str
    }

    func rjust(_ length: Int, withStr: String = " ") -> String {
        return withStr.`repeat`(length - self.length) + self
    }
}


extension Date {
    struct Cache {
        static let currentCalendar = Foundation.Calendar.current
    }
    static var currentCalendar: Foundation.Calendar {
        return Cache.currentCalendar
    }
    static var nowTime: Date {
        let calendar = Date.currentCalendar
        let components = (calendar as NSCalendar).components([.hour, .minute, .second], from:  Date())
        let seconds = components.hour! * 60 * 60 + components.minute! * 60 + components.second!
        return Date(secondsSinceMidnight: seconds)
    }

    init(secondsSinceMidnight seconds: Int) {
        self.init(timeIntervalSince1970: TimeInterval(seconds))
    }

    // date format is "yyyymmdd"
    static func parseDate(asYYYYMMDDInt dateInt: Int) -> Date {
        let calendar = Foundation.Calendar.current
        var com = DateComponents()
        com.year = dateInt / 10000
        com.month = (dateInt / 100) % 100
        com.day = dateInt % 100

        if let date = calendar.date(from: com) {
            return date
        } else {
            fatalError("Can't parse date: \(dateInt)!")
        }
    }

}

extension DateFormatter {
    convenience public init(dateFormat: String!) {
        self.init()
        self.dateFormat = dateFormat
    }

    class func weekDayOf(_ Date: Foundation.Date) -> Int? {
        return Int(DateFormatter(dateFormat: "e").string(from: Date))
    }
}
