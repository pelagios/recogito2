package services.document

import java.util.UUID

/** Helper class for filepart sequence number ordering  **/
case class PartOrdering(partId: UUID, seqNo: Int)
