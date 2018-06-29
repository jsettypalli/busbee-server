
-- This table contains geographical location
--DROP TABLE IF EXISTS geo_location;
CREATE TABLE geo_location (
    id SERIAL PRIMARY KEY,
    latitude decimal(15,12) NOT NULL,
    longitude decimal(15,12) NOT NULL,

    UNIQUE (latitude, longitude)
);

-- DROP TABLE IF EXISTS school;
-- add other attributes viz., address, geo_location, principal, transport_in-charge etc., later
CREATE TABLE school (
    id SERIAL PRIMARY KEY,
    name text NOT NULL,
    address text NULL,
    location_id int NOT NULL,

    UNIQUE (name)
);


-- This table represents bus_stops. This table includes both pickup location and drop-off location of a particular stop.
-- This is to cover the case when drop-off point is little different/away from pickup-point due to one-way or other reason.

--DROP TABLE IF EXISTS bus_stop;
CREATE TABLE bus_stop (
    id SERIAL PRIMARY KEY,
    school_id int NOT NULL,
    name text NOT NULL,
    address text NULL,
    count int NOT NULL DEFAULT 0,
    starting_point boolean NULL,
    wait_time_mins int NOT NULL DEFAULT 0,

    UNIQUE (name, school_id),
    FOREIGN KEY (school_id) REFERENCES school(id)
);

-- DROP TABLE IF EXISTS bus_stop_location;
CREATE TABLE bus_stop_details (
    id SERIAL PRIMARY KEY,
    bus_stop_id int NOT NULL,
    location_id int NOT NULL,
    is_pickup boolean NOT NULL,

    UNIQUE (location_id, is_pickup, bus_stop_id),
    FOREIGN KEY (location_id) REFERENCES geo_location(id),
    FOREIGN KEY (bus_stop_id) REFERENCES bus_stop(id)
);


-- TODO - CREATE TABLE TO STORE STARTING POINTS FOR EACH SCHOOL. AND INDICATE STARTING_POINT IN THE VEHICLE TABLE TOO.
--      THIS IS TO COVER THE CASE WHERE SCHOOL HIRES PARKING PLACES IN DIFF PARTS OF CITY CLOSE TO FIRST PICKUP POINT AND LAST DROPOFF POINT.
--      ADD location_id column to school table to know the location of school

--DROP TABLE IF EXISTS  vehicle;
CREATE TABLE vehicle (
    id SERIAL PRIMARY KEY,
    school_id int NOT NULL,
    start_bus_stop_id int NOT NULL,
    bus_number text NULL,
    registration_number text NOT NULL,
    make text,
    model text,
    capacity int NOT NULL,
    description text NULL,

    FOREIGN KEY (school_id) REFERENCES school(id),
    FOREIGN KEY (start_bus_stop_id) REFERENCES bus_stop(id),
    UNIQUE (registration_number)
);

-- reference table containing route meta info
-- status represents if authorized or not
-- DROP TABLE IF EXISTS  trip;
CREATE TABLE trip (
    id SERIAL PRIMARY KEY,
    school_id int NOT NULL,
    name text NOT NULL,
    schedule text NOT NULL,
    is_pickup boolean NOT NULL,
    approved boolean NOT NULL default false,

    FOREIGN KEY (school_id) REFERENCES school(id),
    UNIQUE (name, school_id, is_pickup)
);

-- table to persist schedule for given route.
-- usually, there would be one schedule for a route.
-- but having schedule info to diff table, gives us flexibility if vehicle has to be run in the same route
--      multiple times in a day.
-- schedule field is expected to contain text in cron format. The time in schedule represents starting time of the vehicle
-----------CREATE TABLE route_schedule (
-----------    id SERIAL PRIMARY KEY,
-----------    schedule text NOT NULL,
-----------    route_id int NOT NULL,

-----------    FOREIGN KEY (route_id) REFERENCES route(id)
-----------);

-- DROP TABLE IF EXISTS  route_map;
-- table containing the map of the route.
-- this contains the pickup points, their order and arrival/departure times relative to the starting time.
CREATE TABLE route_map (
    id SERIAL PRIMARY KEY,
    trip_id int NOT NULL,
    vehicle_id int NOT NULL,
    bus_stop_details_id int NOT NULL,
    bus_stop_order int NOT NULL,
    starting_point boolean NULL,
    destination_point boolean NULL,
    relative_arrival_time_secs int NOT NULL,
    relative_departure_time_secs int NOT NULL,
    relative_distance_mtrs int NULL,

    UNIQUE (trip_id, vehicle_id, bus_stop_details_id, bus_stop_order),
    FOREIGN KEY (trip_id) REFERENCES trip(id),
    FOREIGN KEY (vehicle_id) REFERENCES vehicle(id),
    FOREIGN KEY (bus_stop_details_id) REFERENCES bus_stop_details(id)
);

-- DROP TABLE IF EXISTS  route_plan;
-- Contains daily route plans.
-- daily job runs and generates plan for the day based on route_schedule and route_map info
-- monitoring and notifying users of arrival of their pickup-vehicle is based on this info.
-- the actual arrival/departure info is also captured here.
-- status field can have one of 3 values (YET-TO-START --> 1, ON-THE-WAY --> 2, COMPLETED --> 3)
CREATE TABLE route_plan (
    id SERIAL PRIMARY KEY,
    trip_id int NOT NULL,
    vehicle_id int NOT NULL,
    bus_stop_details_id int NOT NULL,
    bus_stop_order int NOT NULL,
    starting_point boolean NULL,
    destination_point boolean NULL,
    expected_arrival_time timestamp with time zone NULL,
    expected_departure_time timestamp with time zone NULL,
    actual_arrival_time timestamp with time zone NULL,
    actual_departure_time timestamp with time zone NULL,
    vehicle_status text NULL default 'YET-TO-START',

    UNIQUE (trip_id, vehicle_id, bus_stop_details_id, bus_stop_order),
    FOREIGN KEY (trip_id) REFERENCES trip(id),
    FOREIGN KEY (vehicle_id) REFERENCES vehicle(id),
    FOREIGN KEY (bus_stop_details_id) REFERENCES bus_stop_details(id)
);

--DROP TABLE IF EXISTS  person;
CREATE TABLE person (
    id SERIAL PRIMARY KEY,
    user_name text NOT NULL,
    first_name text NOT NULL,
    last_name text NOT NULL,
    phone_number text NULL,
    email text NOT NULL,

    UNIQUE (phone_number),
    UNIQUE (user_name),
    UNIQUE (email)
);
CREATE UNIQUE INDEX phone_number_unique_index on person (phone_number) WHERE  phone_number IS NOT NULL;

-- DROP TABLE IF EXISTS  student;
CREATE TABLE student(
    id SERIAL PRIMARY KEY,
    person_id int NOT NULL,
    class int NOT NULL,
    school_id int NOT NULL,

    FOREIGN KEY (school_id) REFERENCES school(id),
    FOREIGN KEY (person_id) REFERENCES person(id)
);

-- DROP TABLE IF EXISTS  student_parent;
CREATE TABLE student_parent(
    id SERIAL PRIMARY KEY,
    student_id int NOT NULL,
    person_id int NOT NULL,
    relation text NOT NULL,

    FOREIGN KEY (student_id) REFERENCES student(id),
    FOREIGN KEY (person_id) REFERENCES person(id)
);

-- DROP TABLE IF EXISTS  student_bus_stop;
-- TODO - Add trigger on this table to increment/decrement "pickup_point.count" column
CREATE TABLE student_bus_stop (
    id SERIAL PRIMARY KEY,
    student_id int NOT NULL,
    bus_stop_id int NOT NULL,

    FOREIGN KEY (student_id) REFERENCES student(id),
    FOREIGN KEY (bus_stop_id) REFERENCES bus_stop(id)
);


CREATE OR REPLACE FUNCTION update_bus_stop_count()
RETURNS TRIGGER
AS $pickup_point$
BEGIN
  WITH student_count as (select bus_stop_id, count(student_id) as scount from student_bus_stop group by bus_stop_id)
    update bus_stop set count = student_count.scount from student_count where id = student_count.bus_stop_id;
END;
$pickup_point$ LANGUAGE plpgsql;

CREATE TRIGGER UPD_BUS_STOP_COUNT_TRG
AFTER INSERT OR UPDATE OR DELETE ON student_bus_stop
FOR EACH STATEMENT
EXECUTE PROCEDURE update_bus_stop_count();

-- DROP TABLE IF EXISTS  driver_vehicle;
CREATE TABLE driver_vehicle (
    id SERIAL PRIMARY KEY,
    person_id int NOT NULL,
    vehicle_id int NOT NULL,

    FOREIGN KEY (person_id) REFERENCES person(id),
    FOREIGN KEY (vehicle_id) REFERENCES vehicle(id)
);

-- DROP TABLE IF EXISTS  driver_vehicle;
CREATE TABLE transport_incharge (
    id SERIAL PRIMARY KEY,
    person_id int NOT NULL,
    school_id int NOT NULL,

    FOREIGN KEY (person_id) REFERENCES person(id),
    FOREIGN KEY (school_id) REFERENCES school(id)
);

-- This table contains notification settings
--DROP TABLE IF EXISTS settings_notification;
CREATE TABLE settings_notification (
    person_id int NOT NULL,
    notification_time int NOT NULL,

    UNIQUE (person_id,notification_time),
    FOREIGN KEY (person_id) REFERENCES person(id)
  );

-- ==================================

CREATE TABLE user_device_details (
    id SERIAL PRIMARY KEY,
    person_id int NOT NULL,
    platform text NOT NULL,
    device_id text NOT NULL,
    app_token text NOT NULL,
    endpointarn text NULL,

    UNIQUE (person_id, platform, device_id, app_token),
    FOREIGN KEY (person_id) REFERENCES person(id)
);

CREATE TABLE on_call (
    id SERIAL PRIMARY KEY,
    person_id int NOT NULL,

    FOREIGN KEY (person_id) REFERENCES person(id)
);

ALTER TABLE route_map ADD COLUMN via_points text NULL;

-- This table contains via_points between bus stops.
-- The reason is that MapMyIndia or Google is not guaranteed to give the SAME route
--      if query is submitted to get route between two bus stops at different times.
-- Both services return the best route at that point of time, which could be different from what they returned last time.
-- inputting the constraint to cover via_points along the route the bus travels, ensures that SAME route is returned by the service.
-- Again there is small risk that route between via_points could vary, but the chances are very less.
-- We can further reduce the error by adding maximum of 10 via_points between two Bus Stops.
-- Google allows maximum of 10, where as MapMyIndia allows maximum of 16 via_points.
CREATE TABLE input_via_points (
    id SERIAL PRIMARY KEY,
    from_bus_stop_details_id int NOT NULL,
    to_bus_stop_details_id int NOT NULL,
    via_point int NOT NULL,
    via_point_order int NOT NULL,

    UNIQUE (from_bus_stop_details_id, to_bus_stop_details_id, via_point),
    FOREIGN KEY (from_bus_stop_details_id) REFERENCES bus_stop_details(id),
    FOREIGN KEY (to_bus_stop_details_id) REFERENCES bus_stop_details(id),
    FOREIGN KEY (via_point) REFERENCES geo_location(id)
);

ALTER TABLE person ADD COLUMN nick_name text NULL;
-- expected to contain absolute path to the thumbnail image
ALTER TABLE person ADD COLUMN thumbnail_image text NULL;
-- expected to contain absolute path to the large image
ALTER TABLE person ADD COLUMN regular_image text NULL;
-- ===================================================

-- add start_time column to route_map table to set start_time of each route
--      Per the data from meridian schol, each bus has different start time. So, system should accommodate that
--      Hence it is decided that schedule field of trip will only contain date info. time part of that field will be ignored.
--      Instead, time part will be picked from from this field for each bus route.
ALTER TABLE route_map ADD COLUMN start_time time with time zone NULL;

-- ===========================
-- alter table route_map add column relative_distance_mtrs int NULL;
-- alter table route_map add column destination_point boolean NULL;
-- alter table route_plan add column destination_point boolean NULL;

-- alter table route_map drop constraint route_map_trip_id_vehicle_id_bus_stop_details_id_key;
-- CREATE UNIQUE INDEX route_map_trip_id_vehicle_id_bus_stop_details_id_bus_stop_order_key ON route_map (trip_id, vehicle_id, bus_stop_details_id, bus_stop_order);

-- alter table route_plan drop constraint route_plan_trip_id_bus_stop_details_id_bus_stop_order_key;
-- CREATE UNIQUE INDEX route_plan_trip_id_vehicle_id_bus_stop_details_id_bus_stop_order_key ON route_plan (trip_id, vehicle_id, bus_stop_details_id, bus_stop_order);

-- ===========================

-------add via points between 3 Cube Towers and Gem Ascentia (ramalayam and hi-tex arch)
--insert into geo_location (latitude, longitude) values (17.457904, 78.370599);
--insert into geo_location (latitude, longitude) values (17.456698, 78.376726);

------ add way input_via_points info to table
--insert into input_via_points (from_bus_stop_details_id, to_bus_stop_details_id, via_point, via_point_order)
--values (5, 7, 19, 1);
--insert into input_via_points (from_bus_stop_details_id, to_bus_stop_details_id, via_point, via_point_order)
--values (5, 7, 20, 2);
-- ===========================
