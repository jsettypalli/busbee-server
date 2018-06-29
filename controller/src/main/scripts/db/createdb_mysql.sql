
DROP TABLE IF EXISTS geo_location;
CREATE TABLE geo_location (
    id int NOT NULL AUTO_INCREMENT,
    latitude decimal(15,12) NOT NULL,
    longitude decimal(15,12) NOT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY (latitude, longitude)
);

DROP TABLE IF EXISTS pickup_point;
CREATE TABLE pickup_point (
    id int NOT NULL AUTO_INCREMENT,
    location_id int NOT NULL,
    address nvarchar(512),
    count int NOT NULL,
    starting_point boolean NULL,

    PRIMARY KEY (id),
    UNIQUE KEY (location_id)
    FOREIGN KEY (location_id) REFERENCES geo_location(id)
);

DROP TABLE IF EXISTS  vehicle;
CREATE TABLE vehicle (
    id int NOT NULL AUTO_INCREMENT,
    registration_number nvarchar(64) NOT NULL,
    make nvarchar (64),
    model nvarchar (64),
    capacity int NOT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY (registration_number)
);

DROP TABLE IF EXISTS  vehicle_position;
CREATE TABLE vehicle_position (
    id int NOT NULL AUTO_INCREMENT,
    vehicle_id int NOT NULL,
    pickup_point_id int NOT NULL,
    arrival_time DATETIME NOT NULL,

    PRIMARY KEY (id),
    FOREIGN KEY (vehicle_id) REFERENCES vehicle(id),
    FOREIGN KEY (pickup_point_id) REFERENCES pickup_point(id)
);

DROP TABLE IF EXISTS  vehicle_route;
CREATE TABLE vehicle_route(
    id int NOT NULL AUTO_INCREMENT,
    vehicle_id int NOT NULL,
    pickup_point_id int NOT NULL,
    pickup_order int NOT NULL,
    starting_point boolean NULL,

    PRIMARY KEY (id),
    UNIQUE KEY (vehicle_id, pickup_point_id, pickup_order),
    FOREIGN KEY (vehicle_id) REFERENCES vehicle(id),
    FOREIGN KEY (pickup_point_id) REFERENCES pickup_point(id)
);

