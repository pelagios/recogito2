package controllers.document.settings.actions

import controllers.document.BackupWriter
import controllers.document.settings.SettingsController
import models.user.Roles._

trait BackupActions extends BackupWriter { self: SettingsController =>

  private val TMP_DIR = System.getProperty("java.io.tmpdir")

  def exportAsZip(documentId: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    documentAdminAction(documentId, loggedIn.user.getUsername, { doc =>
      createBackup(doc)(self.ctx, self.uploads, self.annotations).map { file =>
        Ok.sendFile(file)
      }
    })
  }

}
