package models.place

import models.geotag.ESGeoTagStore

/** Just an object based on ESGeoTagStore.
  *
  * ESGeoTagStore inherits from ESPlaceStore (which, in turn, is a an ES implementation
  * of PlaceStore combined with PlaceImporter functionality - i.e. the full package).
  */
object PlaceService extends ESGeoTagStore
