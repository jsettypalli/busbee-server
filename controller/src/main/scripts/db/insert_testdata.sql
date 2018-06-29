-- -----------------------------------------------
-- Execute this section first.
-- -----------------------------------------------
insert into school (name) values ('Maharshi Vidya Mandir');

insert into vehicle (registration_number, make, model, capacity) values ('AP 09 CT 1035', 'TestMake', 'TestModel', 50);

-- Bhavya's Alluri Meadows pickup & dropoff
insert into geo_location (latitude, longitude) values (17.453597, 78.366742);
-- 3 Cube Towers pickup & dropoff
insert into geo_location (latitude, longitude) values (17.454906, 78.370133);
-- Gem Ascentia pickup
insert into geo_location (latitude, longitude) values (17.466546, 78.374916);
-- Gem Ascentia drop off
insert into geo_location (latitude, longitude) values (17.466528, 78.375419);
-- Alankruta Residency pickup & dropoff
insert into geo_location (latitude, longitude) values (17.466533, 78.376466);
-- Aditya Sunshine pickup & dropoff
insert into geo_location (latitude, longitude) values (17.466342, 78.371881);
-- Casa Rogue pickup
insert into geo_location (latitude, longitude) values (17.462847, 78.369934);
-- Casa Rogue dropoff
insert into geo_location (latitude, longitude) values (17.463815, 78.369597);
-- Aparna Towers pickup & dropoff
insert into geo_location (latitude, longitude) values (17.464306, 78.368790);
-- Maharshi Vidya Mandir
insert into geo_location (latitude, longitude) values (17.468709, 78.370074);

insert into bus_point (name, starting_point, wait_time_mins) values ('Bhavya''s Alluri meadows', false, 2); -- 4 students
insert into bus_point (name, starting_point, wait_time_mins) values ('3 Cube Towers', false, 2); -- 4 students
insert into bus_point (name, starting_point, wait_time_mins) values ('Gem Ascentia', false, 2); -- 2 students
insert into bus_point (name, starting_point, wait_time_mins) values ('Alankruta Residency', false, 2); -- 3 students
insert into bus_point (name, starting_point, wait_time_mins) values ('Aditya Sunshine', false, 2); -- 6 students
insert into bus_point (name, starting_point, wait_time_mins) values ('Casa Roge', false, 2); -- 6 students
insert into bus_point (name, starting_point, wait_time_mins) values ('Aparna Towers', false, 2); -- 9 students
insert into bus_point (name, starting_point, wait_time_mins) values ('Maharshi Vidya Mandir', true, 2); -- school

-- -----------------------------------------------
-- Execute this section next.
--  get id from geo_location table after above insertion and use them in the insert sql of bus_point_location table
-- -----------------------------------------------
--  Bhavya's Alluri Meadows
insert into bus_point_location (bus_point_location_id, is_pickup, bus_point_id) values (1, true, 1);
insert into bus_point_location (bus_point_location_id, is_pickup, bus_point_id) values (1, false, 1);

-- 3 Cube Towers
insert into bus_point_location (bus_point_location_id, is_pickup, bus_point_id) values (2, true, 2);
insert into bus_point_location (bus_point_location_id, is_pickup, bus_point_id) values (2, false, 2);

-- Gem Ascentia
insert into bus_point_location (bus_point_location_id, is_pickup, bus_point_id) values (3, true, 3);
insert into bus_point_location (bus_point_location_id, is_pickup, bus_point_id) values (4, false, 3);

-- Alankruta Residency
insert into bus_point_location (bus_point_location_id, is_pickup, bus_point_id) values (5, true, 4);
insert into bus_point_location (bus_point_location_id, is_pickup, bus_point_id) values (5, false, 4);

-- Aditya Sunshine
insert into bus_point_location (bus_point_location_id, is_pickup, bus_point_id) values (6, true, 5);
insert into bus_point_location (bus_point_location_id, is_pickup, bus_point_id) values (6, false, 5);

-- Casa Roge
insert into bus_point_location (bus_point_location_id, is_pickup, bus_point_id) values (7, true, 6);
insert into bus_point_location (bus_point_location_id, is_pickup, bus_point_id) values (8, false, 6);

-- Aparna Towers
insert into bus_point_location (bus_point_location_id, is_pickup, bus_point_id) values (9, true, 7);
insert into bus_point_location (bus_point_location_id, is_pickup, bus_point_id) values (9, false, 7);

-- Maharshi Vidya Mandir
insert into bus_point_location (bus_point_location_id, is_pickup, bus_point_id) values (10, true, 8);
insert into bus_point_location (bus_point_location_id, is_pickup, bus_point_id) values (10, false, 8);

-- -----------------------------------------------
-- Execute this section next.
-- -----------------------------------------------
-- to be boarded at 'Bhavya''s Alluri meadows'
insert into person(first_name, last_name, phone_number) values ('student1', 'lname1', '9701234938');
insert into person(first_name, last_name, phone_number) values ('student2', 'lname2', '9701234938');
insert into person(first_name, last_name, phone_number) values ('student3', 'lname3', '9701234938');
insert into person(first_name, last_name, phone_number) values ('student4', 'lname4', '9701234938');
-- to be boarded at '3 Cube Towers'
insert into person(first_name, last_name, phone_number) values ('student5', 'lname5', '9701234938');
insert into person(first_name, last_name, phone_number) values ('student6', 'lname6', '9701234938');
insert into person(first_name, last_name, phone_number) values ('student7', 'lname7', '9701234938');
insert into person(first_name, last_name, phone_number) values ('student8', 'lname8', '9701234938');
-- to be boarded at 'Gem Ascentia'
insert into person(first_name, last_name, phone_number) values ('student9', 'lname9', '9701234938');
insert into person(first_name, last_name, phone_number) values ('student10', 'lname10', '9701234938');
-- to be boarded at 'Alankruta Residency'
insert into person(first_name, last_name, phone_number) values ('student11', 'lname11', '9701234938');
insert into person(first_name, last_name, phone_number) values ('student12', 'lname12', '9701234938');
insert into person(first_name, last_name, phone_number) values ('student13', 'lname13', '9701234938');
-- to be boarded at 'Aditya Sunshine'
insert into person(first_name, last_name, phone_number) values ('student14', 'lname14', '9701234938');
insert into person(first_name, last_name, phone_number) values ('student15', 'lname15', '9701234938');
insert into person(first_name, last_name, phone_number) values ('student16', 'lname16', '9701234938');
insert into person(first_name, last_name, phone_number) values ('student17', 'lname17', '9701234938');
insert into person(first_name, last_name, phone_number) values ('student18', 'lname18', '9701234938');
insert into person(first_name, last_name, phone_number) values ('student19', 'lname19', '9701234938');

-- to be boarded at 'Casa Roge'
insert into person(first_name, last_name, phone_number) values ('student20', 'lname20', '9701234938');
insert into person(first_name, last_name, phone_number) values ('student21', 'lname21', '9701234938');
insert into person(first_name, last_name, phone_number) values ('student22', 'lname22', '9701234938');
insert into person(first_name, last_name, phone_number) values ('student23', 'lname23', '9701234938');
insert into person(first_name, last_name, phone_number) values ('student24', 'lname24', '9701234938');
insert into person(first_name, last_name, phone_number) values ('student25', 'lname25', '9701234938');

-- to be boarded at 'Aparna Towers'
insert into person(first_name, last_name, phone_number) values ('student26', 'lname26', '9701234938');
insert into person(first_name, last_name, phone_number) values ('student27', 'lname27', '9701234938');
insert into person(first_name, last_name, phone_number) values ('student28', 'lname28', '9701234938');
insert into person(first_name, last_name, phone_number) values ('student29', 'lname29', '9701234938');
insert into person(first_name, last_name, phone_number) values ('student30', 'lname30', '9701234938');
insert into person(first_name, last_name, phone_number) values ('student31', 'lname31', '9701234938');
insert into person(first_name, last_name, phone_number) values ('student32', 'lname32', '9701234938');
insert into person(first_name, last_name, phone_number) values ('student33', 'lname33', '9701234938');
insert into person(first_name, last_name, phone_number) values ('student34', 'lname34', '9701234938');

insert into person(first_name, last_name, phone_number) values ('parent1', 'lname', '9701234938');
insert into person(first_name, last_name, phone_number) values ('parent2', 'lname', '9701234938');
insert into person(first_name, last_name, phone_number) values ('parent3', 'lname', '9701234938');
insert into person(first_name, last_name, phone_number) values ('parent4', 'lname', '9701234938');
insert into person(first_name, last_name, phone_number) values ('parent5', 'lname', '9701234938');
insert into person(first_name, last_name, phone_number) values ('parent6', 'lname', '9701234938');
insert into person(first_name, last_name, phone_number) values ('parent7', 'lname', '9701234938');
insert into person(first_name, last_name, phone_number) values ('parent8', 'lname', '9701234938');
insert into person(first_name, last_name, phone_number) values ('parent9', 'lname', '9701234938');
insert into person(first_name, last_name, phone_number) values ('parent10', 'lname', '9701234938');
insert into person(first_name, last_name, phone_number) values ('parent11', 'lname', '9701234938');
insert into person(first_name, last_name, phone_number) values ('parent12', 'lname', '9701234938');
insert into person(first_name, last_name, phone_number) values ('parent13', 'lname', '9701234938');
insert into person(first_name, last_name, phone_number) values ('parent14', 'lname', '9701234938');
insert into person(first_name, last_name, phone_number) values ('parent15', 'lname', '9701234938');
insert into person(first_name, last_name, phone_number) values ('parent16', 'lname', '9701234938');
insert into person(first_name, last_name, phone_number) values ('parent17', 'lname', '9701234938');
insert into person(first_name, last_name, phone_number) values ('parent18', 'lname', '9701234938');
insert into person(first_name, last_name, phone_number) values ('parent19', 'lname', '9701234938');
insert into person(first_name, last_name, phone_number) values ('parent20', 'lname', '9701234938');

-- -----------------------------------------------
-- Execute this section next.
--  get id from person table after above insertion and use them in the insert sql of student table
-- -----------------------------------------------
insert into student (person_id, class, school_id) values (1, 1, 1);
insert into student (person_id, class, school_id) values (2, 1, 1);
insert into student (person_id, class, school_id) values (3, 1, 1);
insert into student (person_id, class, school_id) values (4, 1, 1);
insert into student (person_id, class, school_id) values (5, 1, 1);
insert into student (person_id, class, school_id) values (6, 1, 1);
insert into student (person_id, class, school_id) values (7, 1, 1);
insert into student (person_id, class, school_id) values (8, 1, 1);
insert into student (person_id, class, school_id) values (9, 1, 1);
insert into student (person_id, class, school_id) values (10, 1, 1);

insert into student (person_id, class, school_id) values (11, 1, 1);
insert into student (person_id, class, school_id) values (12, 1, 1);
insert into student (person_id, class, school_id) values (13, 1, 1);
insert into student (person_id, class, school_id) values (14, 1, 1);
insert into student (person_id, class, school_id) values (15, 1, 1);
insert into student (person_id, class, school_id) values (16, 1, 1);
insert into student (person_id, class, school_id) values (17, 1, 1);
insert into student (person_id, class, school_id) values (18, 1, 1);
insert into student (person_id, class, school_id) values (19, 1, 1);
insert into student (person_id, class, school_id) values (20, 1, 1);

insert into student (person_id, class, school_id) values (21, 1, 1);
insert into student (person_id, class, school_id) values (22, 1, 1);
insert into student (person_id, class, school_id) values (23, 1, 1);
insert into student (person_id, class, school_id) values (24, 1, 1);
insert into student (person_id, class, school_id) values (25, 1, 1);
insert into student (person_id, class, school_id) values (26, 1, 1);
insert into student (person_id, class, school_id) values (27, 1, 1);
insert into student (person_id, class, school_id) values (28, 1, 1);
insert into student (person_id, class, school_id) values (29, 1, 1);
insert into student (person_id, class, school_id) values (30, 1, 1);

insert into student (person_id, class, school_id) values (31, 1, 1);
insert into student (person_id, class, school_id) values (32, 1, 1);
insert into student (person_id, class, school_id) values (33, 1, 1);
insert into student (person_id, class, school_id) values (34, 1, 1);

-- -----------------------------------------------
-- Execute this section next.
--  get id's from person table after above insertion and use them in the insert sql of student_parent table
-- -----------------------------------------------
insert into student_parent (student_id, person_id, relation) values (1, 35, 'father');
insert into student_parent (student_id, person_id, relation) values (2, 35, 'father');
insert into student_parent (student_id, person_id, relation) values (3, 35, 'father');
insert into student_parent (student_id, person_id, relation) values (4, 35, 'father');
insert into student_parent (student_id, person_id, relation) values (5, 35, 'father');
insert into student_parent (student_id, person_id, relation) values (6, 36, 'father');
insert into student_parent (student_id, person_id, relation) values (7, 36, 'father');
insert into student_parent (student_id, person_id, relation) values (8, 36, 'father');
insert into student_parent (student_id, person_id, relation) values (9, 36, 'father');
insert into student_parent (student_id, person_id, relation) values (10, 36, 'father');

insert into student_parent (student_id, person_id, relation) values (11, 37, 'father');
insert into student_parent (student_id, person_id, relation) values (12, 37, 'father');
insert into student_parent (student_id, person_id, relation) values (13, 37, 'father');
insert into student_parent (student_id, person_id, relation) values (14, 37, 'father');
insert into student_parent (student_id, person_id, relation) values (15, 37, 'father');
insert into student_parent (student_id, person_id, relation) values (16, 38, 'father');
insert into student_parent (student_id, person_id, relation) values (17, 38, 'father');
insert into student_parent (student_id, person_id, relation) values (18, 38, 'father');
insert into student_parent (student_id, person_id, relation) values (19, 38, 'father');
insert into student_parent (student_id, person_id, relation) values (20, 38, 'father');

insert into student_parent (student_id, person_id, relation) values (21, 39, 'father');
insert into student_parent (student_id, person_id, relation) values (22, 39, 'father');
insert into student_parent (student_id, person_id, relation) values (23, 39, 'father');
insert into student_parent (student_id, person_id, relation) values (24, 39, 'father');
insert into student_parent (student_id, person_id, relation) values (25, 39, 'father');
insert into student_parent (student_id, person_id, relation) values (26, 40, 'father');
insert into student_parent (student_id, person_id, relation) values (27, 40, 'father');
insert into student_parent (student_id, person_id, relation) values (28, 40, 'father');
insert into student_parent (student_id, person_id, relation) values (29, 40, 'father');
insert into student_parent (student_id, person_id, relation) values (30, 40, 'father');

insert into student_parent (student_id, person_id, relation) values (31, 41, 'father');
insert into student_parent (student_id, person_id, relation) values (32, 41, 'father');
insert into student_parent (student_id, person_id, relation) values (33, 41, 'father');
insert into student_parent (student_id, person_id, relation) values (34, 41, 'father');

-- -----------------------------------------------
-- Execute this section next.
--  get id's from person table after above insertion and use them in the insert sql of student_parent table
-- -----------------------------------------------


-- PLACE HOLDER SQLS. SHOULD BE DELETED LATER
-- --------------------------------------------
-- alter table person add column email text NOT NULL;
-- alter table person alter column phone_number drop not null;
-- create unique index phone_number_unique_index on person (phone_number) where  phone_number is not null;

-- insert into person (first_name, last_name, phone_number, email) values ('driver1', '', '+919701234938', 'driver1@email.com');
-- insert into person (first_name, last_name, email) values ('parent1', '',  'parent1@email.com');
-- insert into person (first_name, last_name, email) values ('transport_incharge1', '', 'transport_incharge1@email.com');
-- insert into person (first_name, last_name, email) values ('student1', '', 'student1@email.com');
-- insert into driver_vehicle (person_id, vehicle_id) values (1, 2);

-- insert into student (person_id, class, school_id) values (5, 10, 1);

-- insert into student_parent(student_id, person_id, relation) values (1, 3, 'Father');

-- insert into student_bus_stop (student_id, bus_stop_id) values (1, 3);

-- insert into transport_incharge (person_id, school_id) values (4, 1);