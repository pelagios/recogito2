package models.place

import models.geotag.ESGeoTagStore

/** Just combines the functionality of ESPlaceStore with that of ESGeoTagStore in an object **/ 
object PlaceService extends ESPlaceStore with ESGeoTagStore
