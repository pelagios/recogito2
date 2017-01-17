define([
  'common/config',
  'document/annotation/common/page/baseToolbar'
], function(Config, BaseToolbar) {

  var Toolbar = function(rootNode) {

    var self = this,

        initBulkMenu = function() {
          jQuery('.bulk-annotation').on('click', 'li', function(e) {
            var t = jQuery(e.target).closest('li'),
                disabled = t.hasClass('disabled'),
                type = t.data('type');

            if (!disabled)
              self.fireEvent('bulkAnnotation', type);

            return false;
          });
        },

        disableAnnotationControls = function() {
          jQuery('.bulk-annotation').addClass('disabled');
        };

    if (Config.writeAccess)
      initBulkMenu();
    else
      disableAnnotationControls();

    BaseToolbar.apply(this, [ rootNode ]);
  };
  Toolbar.prototype = Object.create(BaseToolbar.prototype);

  return Toolbar;

});
