package models.contribution

import org.joda.time.DateTime

case class ContributionStats(
    
  took: Long,
    
  totalContributions: Long,
  
  byUser: Seq[(String, Long)],
  
  byAction: Seq[(ContributionAction.Value, Long)],
  
  byItemType: Seq[(ItemType.Value, Long)],
  
  contributionHistory: Seq[(DateTime, Long)]
  
)