define([
  'document/annotation/common/page/baseToolbar'
], function(BaseToolbar) {

  var Toolbar = function(rootNode) {
    BaseToolbar.apply(this, [ rootNode ]);
  };
  Toolbar.prototype = Object.create(BaseToolbar.prototype);

  return Toolbar;

});
