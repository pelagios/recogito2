package models.place

import models.geotag.ESGeoTagStore
import storage.HasES
import javax.inject.{ Singleton, Inject }
import storage.ES
import models.geotag.ESGeoTagStore

@Singleton
class PlaceService @Inject() (val es: ES) extends ESPlaceStore with ESGeoTagStore with HasES
