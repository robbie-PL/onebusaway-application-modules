/*
 * Copyright (C) 2016 Cambridge Systematics, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
function createObaModule(title, urlinput, timestamp, latlon, trip, route, block, schdev, nextstopid, 
		nextstoppred, finalstopid, finalstoppred, nexttripid, age, mapName) {
	
	var obaWeb = OBA.config.obaUrl || "app.dev.wmata.obaweb.org:8080";
	urlinput.val(obaWeb);
	
	var obaApiPrefix = OBA.config.obaApiPrefix || "";
	
	title.html("OBA Position")
	
	var obaAge = null;
	
	var obaAttrs = {};
	
	nextstopid.html('<label id="oba_tdsnextstopid">loading...</label> (tds) / <label id="oba_sirinextstopid">loading...</label> (siri)');
	nextstoppred.html('<label class="stoppred" id="oba_tdsnextstoppred">loading...</label> (tds) / <label class="stoppred" id="oba_sirinextstoppred">loading...</label> (siri)');
	finalstoppred.html('...');
	
	var tdsnextstopid = jQuery("#oba_tdsnextstopid");
	var sirinextstopid = jQuery("#oba_sirinextstopid");
	var tdsnextstoppred = jQuery("#oba_tdsnextstoppred");
	var sirinextstoppred = jQuery("#oba_sirinextstoppred");
	
	// unavailable info
	block.html("N/A");
	
	var module = {};
	
	module.refresh = function(agencyId, beginDate, numDays, vehicleId, beginTime) {
		
		obaWeb = urlinput.val();
	
		var obaUrl = "http://" + obaWeb + "/onebusaway-api-webapp/siri/vehicle-monitoring?key=OBAKEY&OperatorRef="
			+ agencyId + "&VehicleRef=" + vehicleId +  "&type=json";
		
		jQuery.ajax({
			url: obaUrl,
			jsonp: "callback",
			dataType: "jsonp",
			type: "GET",
			async: false,
			success: function(response) {
				var attrs = parseSiri(response)
				loadObaMap(attrs['latLng']);
				queryOBAApiValues(attrs, agencyId, vehicleId);
				queryOBAFinalStop(attrs, agencyId, vehicleId);
				obaAttrs = attrs;
			},
			fail: function() {
				schdev.html("...");
				tdsnextstopid.html("...");
				tdsnextstoppred.html("...");
				finalstopid.html("...");
				nexttripid.html("...");
			}
		});
		
	}
	
	function loadObaMap(latLng) {
		if (latLng != null) {
			loadMap(latLng, null, mapName);
		}
	}
	
	function queryOBAApiValues(attrs, agencyId, vehicleId) {

		var apiUrl = "http://" + obaWeb + obaApiPrefix
			+ "/api/where/trip-for-vehicle/"
			+ agencyId + "_" + vehicleId + ".json?key=OBAKEY";

		jQuery.ajax({
			url: apiUrl,
			type: "GET",
			jsonp: "callback",
			dataType: "jsonp",
			async: false,
			success: function(response) {
				if (response.data != undefined && response.data.entry != undefined 
						&& response.data.entry.status != undefined) {
					var now = new Date(response.currentTime);	
					var e = response.data.entry;
					var s = e.status;
					attrs["schedDev"] = formatScheduleDeviation(s.scheduleDeviation);
					schdev.html(formatScheduleDeviation(s.scheduleDeviation))
					attrs["tdsNextStopId"] = s.nextStop.split("_")[1];
					tdsnextstopid.html(s.nextStop.split("_")[1].toString());
					attrs["tdsLastUpdate"] = new Date(s.lastUpdateTime);
					setAge(s.lastUpdateTime);
					
					queryOBANextStopPrediction(attrs, agencyId, attrs["tdsNextStopId"], vehicleId)
				} else {
					schdev.html("...");
					tdsnextstopid.html("...");
					tdsnextstoppred.html("...");
				}
			},
			fail: function() {
				schdev.html("...");
				tdsnextstopid.html("...");
				tdsnextstoppred.html("...");
			}
		});	
	}

	function queryOBAFinalStop(attrs, agencyId, vehicleId) {
		var tripId = attrs["tripId"];
		if (tripId == undefined) {
			console.log("missing trip id");
			finalstopid.html("...");
			finalstoppred.html("...");
			nexttripid.html("...");
			return
		}
		var scheduleUrl = "http://" + obaWeb + obaApiPrefix
			+ "/api/where/trip-details/"
			+ agencyId + "_" + tripId + ".json?key=OBAKEY";
		
		jQuery.ajax({
			url: scheduleUrl,
			type: "GET",
			jsonp: "callback",
			dataType: "jsonp",
			async: false,
			success: function(response) {
				var schedule = response.data.entry.schedule;
				var s = response.data.entry.schedule.stopTimes;
				attrs["tdsFinalStopId"] = s[s.length-1].stopId.split("_")[1];
				finalstopid.html(s[s.length-1].stopId.split("_")[1]);
				
				if (schedule != undefined && schedule.nextTripId != undefined) {
					nexttripid.html(schedule.nextTripId.split("_")[1]);
				} else {
					nexttripid.html("end of block");
				}

				
				queryOBAFinalPrediction(attrs, agencyId, attrs["tdsFinalStopId"], vehicleId)
			},
			fail: function() {
				finalstopid.html("...");
				nexttripid.html("...");
			}
		});	
	}

	function queryOBAFinalPrediction(attrs, agencyId, stopId, vehicleId) {
		queryOBAStopPrediction(attrs, agencyId, stopId, vehicleId, true, "tdsFinalStopPred", finalstoppred)
	}

	function queryOBANextStopPrediction(attrs, agencyId, stopId, vehicleId) {
		queryOBAStopPrediction(attrs, agencyId, stopId, vehicleId, false, "tdsNextPrediction", tdsnextstoppred)
	}

	function queryOBAStopPrediction(attrs, agencyId, stopId, vehicleId, isArrival, attrsKey, label) {
		
		var tripId = attrs["tripId"];
		// Setting service date to today so will not work for trips that span a day.
		var serviceDate = new Date(); 
		serviceDate.setHours(0,0,0,0);
		var lastStopPredUrl = "http://" + obaWeb + obaApiPrefix
			+ "/api/where/arrival-and-departure-for-stop/"
			+ agencyId + "_" + stopId + ".json?key=OBAKEY&tripId="
			+ agencyId + "_" + tripId + "&vehicleId=" + agencyId + "_" + vehicleId 
			+ "&serviceDate=" + serviceDate.getTime();

		jQuery.ajax({
			url: lastStopPredUrl,
			type: "GET",
			jsonp: "callback",
			dataType: "jsonp",
			async: false,
			success: function(response) {
				var e = response.data.entry;
				var pred = isArrival ? e.predictedArrivalTime : e.predictedDepartureTime;
				attrs[attrsKey] = new Date(pred);
				label.html(formatTime(new Date(pred)));
			},
			fail: function() {
				label.html("...");
			}
		});
		
	}

	function parseSiri(siri) {
		var attrs = new Object();
		var latLng = null;
		var sd = siri.Siri.ServiceDelivery;
		if (sd != undefined) {
			var vmd = sd.VehicleMonitoringDelivery;
			var va = vmd[0].VehicleActivity;
			if (va != undefined && va.length > 0) {
				var mvj = va[0].MonitoredVehicleJourney;
				if (mvj != undefined) {
					latLng = L.latLng(mvj.VehicleLocation.Latitude, mvj.VehicleLocation.Longitude);
					attrs['pretty_timestamp'] = new Date(sd.ResponseTimestamp);
					timestamp.html(formatTime(new Date(sd.ResponseTimestamp)));
					attrs["latLng"] = latLng
					latlon.html(mvj.VehicleLocation.Latitude + ", " + mvj.VehicleLocation.Longitude)
					attrs["routeId"] = mvj.LineRef.split("_")[1];
					route.html(mvj.LineRef.split("_")[1])
					attrs["tripId"] = mvj.FramedVehicleJourneyRef.DatedVehicleJourneyRef.split("_")[1];
					trip.html(mvj.FramedVehicleJourneyRef.DatedVehicleJourneyRef.split("_")[1])
					// BlockRef is missing
					//jQuery("#oba_block").html(mvj.BlockRef.split("_")[1]);
					if (mvj.MonitoredCall.StopPointRef != undefined) {
						attrs["siriNextStopId"] = mvj.MonitoredCall.StopPointRef.split("_")[1];
						sirinextstopid.html(mvj.MonitoredCall.StopPointRef.split("_")[1]);
					} else {
						attrs["siriNextStopId"] = undefined;
						sirinextstopid.html("...");
					}
					attrs["siriNextPrediction"] = mvj.MonitoredCall.ExpectedDepartureTime;
					sirinextstoppred.html(formatTime(new Date(mvj.MonitoredCall.ExpectedDepartureTime)));
					attrs["siriMonitored"] = mvj.Monitored;
				} else {
					console.log("MonitoringVehicleJourney missing");
				}
			} else {
				console.log("VehiceActivity missing:" + siri.Siri.ServiceDelivery.VehicleMonitoringDelivery);
				timestamp.html("...");
				latlon.html("...");
				route.html("...");
				trip.html("...");
				block.html("...");
				schdev.html("...");
				tdsnextstopid.html("...");
				tdsnextstoppred.html("...");
				sirinextstopid.html("...");
				sirinextstoppred.html("...");
			}
		} else {
			timestamp.html("...");
			latlon.html("...");
			route.html("...");
			trip.html("...");
			block.html("...");
			schdev.html("...");
			tdsnextstopid.html("...");
			tdsnextstoppred.html("...");
			irinextstopid.html("...");
			sirinextstoppred.html("...");
		}
		return attrs;
	}
	
	function setAge(d) {
		obaAge = new Date(d).getTime();
		module.updateAge();
	}
	
	module.updateAge = function() {
		updateAge(age, obaAge)
	}
	

	return module;
}