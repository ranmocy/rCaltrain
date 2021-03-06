//
//  AppDelegate.swift
//  rCaltrain
//
//  Created by Ranmocy on 9/30/14.
//  Copyright (c) 2014-2015 Ranmocy. All rights reserved.
//

import UIKit

@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate {

    var window: UIWindow?

    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplicationLaunchOptionsKey: Any]?) -> Bool {

        // load stops data
        //  stop_name => [stop_id1, stop_id2]
        //  "22ND ST" => [70021, 70022]
        for (name, idsArray) in readPlistAsDict("stops") as! [String: NSArray] {
            for id in idsArray as! [Int] {
                Station.addStation(name: name, id: id)
            }
        }

        // load routes data
        // { route_id => { service_id => { trip_id => [[stop_id, arrival_time/departure_time(in seconds)]] } } }
        // { "Bullet" => { "CT-14OCT-XXX" => { "650770-CT-14OCT-XXX" => [[70012, 29700], ...] } } }
        for (routeName, servicesDict) in readPlistAsDict("routes") as! [String: NSDictionary] {
            Route.addRoute(name: routeName, servicesDict: servicesDict)
        }

        // load calendar data
        // service_id => {weekday: bool, saturday: bool, sunday: bool, start_date: date, end_date: date}
        // 4930 => {weekday: false, saturday: true, sunday: false, start_date: 20160404, end_date: 20190406}
        for (serviceId, dict) in readPlistAsDict("calendar") as! [String: NSDictionary] {
            if let services = Service.getServices(byId: serviceId) {
                let calendar = Calendar(dict: dict)
                for service in services {
                    service.calendar = calendar
                }
            } else {
                fatalError("Can't find service \(serviceId) when load calendar.plist.")
            }
        }

        // load calendar_dates data
        // {serviceID: [exception_date,type]}
        for (serviceId, items) in readPlistAsDict("calendar_dates") as! [String: NSArray] {
            if let services = Service.getServices(byId: serviceId) {
                for item in items as! [NSArray] {
                    let dates = CalendarDates(dateInt: item[0] as! Int, toAdd: item[1] as! Int == 1)
                    for service in services {
                        service.calendar_dates.append(dates)
                    }
                }
            } else {
                fatalError("Can't find service \(serviceId) when load calendar_dates.plist")
            }
        }

        return true
    }

    func applicationWillResignActive(_ application: UIApplication) {
        // Sent when the application is about to move from active to inactive state. This can occur for certain types of temporary interruptions (such as an incoming phone call or SMS message) or when the user quits the application and it begins the transition to the background state.
        // Use this method to pause ongoing tasks, disable timers, and throttle down OpenGL ES frame rates. Games should use this method to pause the game.
    }

    func applicationDidEnterBackground(_ application: UIApplication) {
        // Use this method to release shared resources, save user data, invalidate timers, and store enough application state information to restore your application to its current state in case it is terminated later.
        // If your application supports background execution, this method is called instead of applicationWillTerminate: when the user quits.
    }

    func applicationWillEnterForeground(_ application: UIApplication) {
        // Called as part of the transition from the background to the inactive state; here you can undo many of the changes made on entering the background.
    }

    func applicationDidBecomeActive(_ application: UIApplication) {
        // Restart any tasks that were paused (or not yet started) while the application was inactive. If the application was previously in the background, optionally refresh the user interface.
    }

    func applicationWillTerminate(_ application: UIApplication) {
        // Called when the application is about to terminate. Save data if appropriate. See also applicationDidEnterBackground:.
    }

    fileprivate func readPlistAsDict(_ name: String) -> NSDictionary {
        if let filePath = Bundle.main.path(forResource: name, ofType: "plist") {
            if let dict = NSDictionary(contentsOfFile: filePath) {
                return dict
            } else {
                fatalError("Can't read plist file \(name)!")
            }
        } else {
            fatalError("Can't find plist file \(name)!")
        }
    }

}

