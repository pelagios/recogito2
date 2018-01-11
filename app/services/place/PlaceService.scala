package services.place

import services.geotag.ESGeoTagStore
import storage.HasES
import javax.inject.{ Singleton, Inject }
import storage.ES
import services.geotag.ESGeoTagStore

@Singleton
class PlaceService @Inject() (val es: ES) extends ESPlaceStore with ESGeoTagStore with HasES
