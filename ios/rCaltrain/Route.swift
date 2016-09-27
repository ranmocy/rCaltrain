//
//  Route.swift
//  rCaltrain
//
//  Created by Wanzhang Sheng on 2/17/15.
//  Copyright (c) 2015 Ranmocy. All rights reserved.
//

import Foundation

class Route {

    fileprivate struct RouteStruct {
        static var routesHash = [String: Route]()
    }

    class var allRoutes : [String: Route] {
        get { return RouteStruct.routesHash }
        set { RouteStruct.routesHash = newValue }
    }

    class func addRoute(name : String, servicesDict : NSDictionary) {
        var services = [String: Service]()
        for (serviceId, tripsDict) in servicesDict as! [String: NSDictionary] {
            services[serviceId] = Service.addService(id: serviceId, tripsDict: tripsDict)
        }
        let route = Route(name: name, services: services)
        Route.allRoutes[name] = route
    }


    let name : String
    let services : [String: Service]

    fileprivate init(name : String, services : [String: Service]) {
        self.name = name
        self.services = services
    }

}
