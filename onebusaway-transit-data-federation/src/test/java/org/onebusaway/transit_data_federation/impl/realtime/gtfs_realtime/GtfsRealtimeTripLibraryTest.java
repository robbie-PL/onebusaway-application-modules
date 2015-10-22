/**
 * Copyright (C) 2011 Google, Inc.
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
package org.onebusaway.transit_data_federation.impl.realtime.gtfs_realtime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.onebusaway.transit_data_federation.testing.UnitTestingSupport.block;
import static org.onebusaway.transit_data_federation.testing.UnitTestingSupport.blockConfiguration;
import static org.onebusaway.transit_data_federation.testing.UnitTestingSupport.serviceIds;
import static org.onebusaway.transit_data_federation.testing.UnitTestingSupport.stop;
import static org.onebusaway.transit_data_federation.testing.UnitTestingSupport.stopTime;
import static org.onebusaway.transit_data_federation.testing.UnitTestingSupport.time;
import static org.onebusaway.transit_data_federation.testing.UnitTestingSupport.trip;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.onebusaway.realtime.api.VehicleLocationRecord;
import org.onebusaway.transit_data_federation.impl.transit_graph.BlockEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.StopEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.TripEntryImpl;
import org.onebusaway.transit_data_federation.services.blocks.BlockCalendarService;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockConfigurationEntry;

import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.FeedHeader;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtime.Position;
import com.google.transit.realtime.GtfsRealtime.TripDescriptor;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeEvent;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate;
import com.google.transit.realtime.GtfsRealtime.VehicleDescriptor;
import com.google.transit.realtime.GtfsRealtime.VehiclePosition;
import com.google.transit.realtime.GtfsRealtimeConstants;

public class GtfsRealtimeTripLibraryTest {

  private GtfsRealtimeTripLibrary _library;
  private GtfsRealtimeEntitySource _entitySource;
  private BlockCalendarService _blockCalendarService;

  @Before
  public void before() {
    _library = new GtfsRealtimeTripLibrary();
    _library.setCurrentTime(8 * 60 * 60 * 1000);
    _entitySource = Mockito.mock(GtfsRealtimeEntitySource.class);
    _library.setEntitySource(_entitySource);

    _blockCalendarService = Mockito.mock(BlockCalendarService.class);
    _library.setBlockCalendarService(_blockCalendarService);
  }

  
  @Test
  public void testGroupTripUpdatesAndVehiclePositionsByVehicleId() {
	  FeedMessage.Builder tripUpdates = createFeed();
	  
	  FeedMessage.Builder vehiclePositions = createFeed();
	  
	  // test 0: empty feed
	  List<CombinedTripUpdatesAndVehiclePosition> group = _library.groupTripUpdatesAndVehiclePositions(tripUpdates.build(), vehiclePositions.build());
	  
	  assertEquals(0, group.size());

	  // test 1: vehicle position with no trip update
	 tripUpdates = createFeed();
	 group = _library.groupTripUpdatesAndVehiclePositions(tripUpdates.build(), vehiclePositions.build());
	 assertEquals(0, group.size());
	  
	  tripUpdates = createFeed();
	  FeedEntity tripUpdateEntityA = createTripUpdate("vehicle1", "tripA", "stopA", 60, true, 0);
	  assertTrue(tripUpdateEntityA.hasTripUpdate());
	  assertTrue(tripUpdateEntityA.getTripUpdate().hasVehicle());
	  
	  tripUpdates.addEntity(tripUpdateEntityA);
	  TripEntryImpl tripA = trip("tripA");
	  TripEntryImpl tripB = trip("tripB");
	  StopEntryImpl stopA = stop("stopA", 0, 0);
	  StopEntryImpl stopB = stop("stopB", 0, 0);
	  stopTime(0, stopA, tripA, time(7, 30), 0.0);
	  stopTime(1, stopB, tripB, time(8, 30), 0.0);
	  Mockito.when(_entitySource.getTrip("tripA")).thenReturn(tripA);
	  Mockito.when(_entitySource.getTrip("tripB")).thenReturn(tripB);
      BlockEntryImpl blockA = block("blockA");
      BlockConfigurationEntry blockConfigA = blockConfiguration(blockA,
    	        serviceIds("s1"), tripA, tripB);
      BlockInstance blockInstanceA = new BlockInstance(blockConfigA, 0L);
      Mockito.when(
    	        _blockCalendarService.getActiveBlocks(Mockito.eq(blockA.getId()),
    	            Mockito.anyLong(), Mockito.anyLong())).thenReturn(
    	        Arrays.asList(blockInstanceA));

	  vehiclePositions = createFeed();

	  
	 // test 2: single vehicle update with no vehicle position
	 group = _library.groupTripUpdatesAndVehiclePositions(tripUpdates.build(), vehiclePositions.build());
	 
	 assertEquals(1, group.size());
	 assertEquals(1, group.get(0).tripUpdates.size());
	 assertTrue(group.get(0).tripUpdates.get(0).hasVehicle());
	 assertEquals(null, group.get(0).vehiclePosition);
	  
	 
     FeedEntity.Builder vehiclePositionEntity = FeedEntity.newBuilder();
     VehicleDescriptor.Builder vdp = VehicleDescriptor.newBuilder();
 	 vdp.setId("vehicle1");
 	 VehiclePosition.Builder vp = VehiclePosition.newBuilder();
 	 vp.setVehicle(vdp);
 	 Position.Builder position = Position.newBuilder();
 	 position.setLatitude(-1.0f);
 	 position.setLongitude(-2.0f);
 	 vp.setPosition(position);
     vehiclePositionEntity.setId("1");
     vehiclePositionEntity.setVehicle(vp.build());
     vehiclePositions.addEntity(vehiclePositionEntity);

     // test 3: single vehicle update with a vehicle position but no vehicle position trip
     group = _library.groupTripUpdatesAndVehiclePositions(tripUpdates.build(), vehiclePositions.build());
     assertEquals(1, group.size());
	 assertEquals(1, group.get(0).tripUpdates.size());
	 assertNotNull(group.get(0).vehiclePosition);
	 assertEquals("vehicle1", group.get(0).tripUpdates.get(0).getVehicle().getId());
	 assertEquals("vehicle1", group.get(0).vehiclePosition.getVehicle().getId());

	 
	 // test 4: two vehicle updates that are on same block but different trips, still 1 position
	 FeedEntity tripUpdateEntityB = createTripUpdate("vehicle1", "tripB", "stopB", 120, true, 1);
	 
	 tripUpdates.addEntity(tripUpdateEntityB);
     group = _library.groupTripUpdatesAndVehiclePositions(tripUpdates.build(), vehiclePositions.build());
     // as we key on vehicleId, we take the newest trip update
     assertEquals(1, group.size());
	 assertEquals(1, group.get(0).tripUpdates.size());
	 assertEquals("tripB", group.get(0).tripUpdates.get(0).getTrip().getTripId());
	 assertNotNull(group.get(0).vehiclePosition);
	 assertEquals("vehicle1", group.get(0).tripUpdates.get(0).getVehicle().getId());
	 assertEquals("vehicle1", group.get(0).vehiclePosition.getVehicle().getId());
	 
	 
  }
  

  @Test
  public void testGroupTripUpdatesAndVehiclePositionsByBlock() {

    FeedEntity tripUpdateEntityA = createTripUpdate("tripA", "stopA", 60, true);
    FeedEntity tripUpdateEntityB = createTripUpdate("tripB", "stopB", 120, true);
    FeedEntity tripUpdateEntityC = createTripUpdate("tripC", "stopA", 30, true);
    FeedEntity tripUpdateEntityD = createTripUpdate("tripD", "stopB", 90, true);

    FeedMessage.Builder tripUpdates = createFeed();
    tripUpdates.addEntity(tripUpdateEntityA);
    tripUpdates.addEntity(tripUpdateEntityB);
    tripUpdates.addEntity(tripUpdateEntityC);
    tripUpdates.addEntity(tripUpdateEntityD);

    TripEntryImpl tripA = trip("tripA");
    TripEntryImpl tripB = trip("tripB");
    TripEntryImpl tripC = trip("tripC");
    TripEntryImpl tripD = trip("tripD");

    StopEntryImpl stopA = stop("stopA", 0, 0);
    StopEntryImpl stopB = stop("stopB", 0, 0);

    stopTime(0, stopA, tripA, time(7, 30), 0.0);
    stopTime(1, stopB, tripB, time(8, 30), 0.0);
    stopTime(2, stopA, tripC, time(8, 30), 0.0);
    stopTime(3, stopB, tripD, time(9, 30), 0.0);

    Mockito.when(_entitySource.getTrip("tripA")).thenReturn(tripA);
    Mockito.when(_entitySource.getTrip("tripB")).thenReturn(tripB);
    Mockito.when(_entitySource.getTrip("tripC")).thenReturn(tripC);
    Mockito.when(_entitySource.getTrip("tripD")).thenReturn(tripD);

    BlockEntryImpl blockA = block("blockA");
    BlockEntryImpl blockB = block("blockB");

    BlockConfigurationEntry blockConfigA = blockConfiguration(blockA,
        serviceIds("s1"), tripA, tripB);
    BlockConfigurationEntry blockConfigB = blockConfiguration(blockB,
        serviceIds("s1"), tripC, tripD);

    BlockInstance blockInstanceA = new BlockInstance(blockConfigA, 0L);
    BlockInstance blockInstanceB = new BlockInstance(blockConfigB, 0L);

    Mockito.when(
        _blockCalendarService.getActiveBlocks(Mockito.eq(blockA.getId()),
            Mockito.anyLong(), Mockito.anyLong())).thenReturn(
        Arrays.asList(blockInstanceA));
    Mockito.when(
        _blockCalendarService.getActiveBlocks(Mockito.eq(blockB.getId()),
            Mockito.anyLong(), Mockito.anyLong())).thenReturn(
        Arrays.asList(blockInstanceB));

    FeedMessage.Builder vehiclePositions = createFeed();
    // FeedEntity.Builder vehiclePositionEntity = FeedEntity.newBuilder();
    // vehiclePositions.addEntity(vehiclePositionEntity);

    // here we match based on block, not based on vehicleId
    List<CombinedTripUpdatesAndVehiclePosition> groups = _library.groupTripUpdatesAndVehiclePositions(
        tripUpdates.build(), vehiclePositions.build());

    assertEquals(2, groups.size());

    Collections.sort(groups);

    CombinedTripUpdatesAndVehiclePosition group = groups.get(0);
    assertSame(blockA, group.block.getBlockInstance().getBlock().getBlock());
    assertEquals(2, group.tripUpdates.size());
    TripUpdate tripUpdate = group.tripUpdates.get(0);
    assertEquals("tripA", tripUpdate.getTrip().getTripId());
    tripUpdate = group.tripUpdates.get(1);
    assertEquals("tripB", tripUpdate.getTrip().getTripId());

    group = groups.get(1);
    assertSame(blockB, group.block.getBlockInstance().getBlock().getBlock());
    assertEquals(2, group.tripUpdates.size());
    tripUpdate = group.tripUpdates.get(0);
    assertEquals("tripC", tripUpdate.getTrip().getTripId());
    tripUpdate = group.tripUpdates.get(1);
    assertEquals("tripD", tripUpdate.getTrip().getTripId());

    VehicleLocationRecord record = _library.createVehicleLocationRecordForUpdate(groups.get(0));
    assertEquals(blockA.getId(), record.getBlockId());
    assertEquals(120, record.getScheduleDeviation(), 0.0);
    assertEquals(0L, record.getServiceDate());
    assertEquals(blockA.getId(), record.getVehicleId());

    record = _library.createVehicleLocationRecordForUpdate(groups.get(1));
    assertEquals(blockB.getId(), record.getBlockId());
    assertEquals(30, record.getScheduleDeviation(), 0.0);
    assertEquals(0L, record.getServiceDate());
    assertEquals(blockB.getId(), record.getVehicleId());
  }

  private FeedMessage.Builder createFeed() {
    FeedMessage.Builder builder = FeedMessage.newBuilder();
    FeedHeader.Builder header = FeedHeader.newBuilder();
    header.setGtfsRealtimeVersion(GtfsRealtimeConstants.VERSION);
    builder.setHeader(header);
    return builder;
  }

  private FeedEntity createTripUpdate(String tripId, String stopId, int delay,
	      boolean arrival) {
	  return createTripUpdate(null, tripId, stopId, delay, arrival, 0);
  }
  
  private FeedEntity createTripUpdate(String vehicleId, String tripId, String stopId, int delay,
	      boolean arrival, int timestamp) {
    TripUpdate.Builder tripUpdate = TripUpdate.newBuilder();

    StopTimeUpdate.Builder stopTimeUpdate = StopTimeUpdate.newBuilder();
    if (stopId != null)
      stopTimeUpdate.setStopId(stopId);
    StopTimeEvent.Builder stopTimeEvent = StopTimeEvent.newBuilder();
    stopTimeEvent.setDelay(delay);
    if (arrival) {
      stopTimeUpdate.setArrival(stopTimeEvent);
    } else {
      stopTimeUpdate.setDeparture(stopTimeEvent);
    }

    tripUpdate.addStopTimeUpdate(stopTimeUpdate);

    TripDescriptor.Builder tripDescriptor = TripDescriptor.newBuilder();
    tripDescriptor.setTripId(tripId);
    tripUpdate.setTrip(tripDescriptor);

    FeedEntity.Builder tripUpdateEntity = FeedEntity.newBuilder();
    tripUpdateEntity.setId(tripId);
    
    if (vehicleId != null) {
      VehicleDescriptor.Builder vehicleDescriptor = VehicleDescriptor.newBuilder();
      vehicleDescriptor.setId(vehicleId);
      tripUpdate.setVehicle(vehicleDescriptor);
    }

    tripUpdate.setTimestamp(timestamp);
    tripUpdateEntity.setTripUpdate(tripUpdate);
    return tripUpdateEntity.build();
  }
}
