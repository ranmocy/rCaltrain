//
//  CalendarDates.swift
//  rCaltrain
//
//  Created by Wanzhang Sheng on 2/20/15.
//  Copyright (c) 2015 Ranmocy. All rights reserved.
//

import Foundation

class CalendarDates {

    var exception_date : Date
    var toAdd : Bool

    init(date: Date, toAdd add: Bool) {
        exception_date = date
        toAdd = add
    }

    convenience init(dateInt : Int, toAdd add: Bool) {
        self.init(date: Date.parseDate(asYYYYMMDDInt: dateInt), toAdd: add)
    }

}
