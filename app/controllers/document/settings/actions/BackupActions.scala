package controllers.document.settings.actions

import controllers.document.BackupWriter
import controllers.document.settings.SettingsController
import models.user.Roles._

trait BackupActions extends BackupWriter { self: SettingsController =>

  private val TMP_DIR = System.getProperty("java.io.tmpdir")

  def exportAsZip(documentId: String) = self.silhouette.SecuredAction.async { implicit request =>
    documentAdminAction(documentId, request.identity.username, { doc =>
      createBackup(doc)(self.ctx, self.uploads, self.annotations, self.tmpFile).map { file =>
        Ok.sendFile(file)
      }
    })
  }

}
