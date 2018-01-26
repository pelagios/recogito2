package controllers.document.settings.actions

import controllers.document.BackupWriter
import controllers.document.settings.SettingsController
import services.user.Roles._
import storage.TempDir

trait BackupActions extends BackupWriter { self: SettingsController =>

  def exportAsZip(documentId: String) = self.silhouette.SecuredAction.async { implicit request =>
    documentAdminAction(documentId, request.identity.username, { doc =>
      createBackup(doc)(self.ctx, self.uploads, self.annotations, self.tmpFile).map { file =>
        Ok.sendFile(file)
      }
    })
  }

}
