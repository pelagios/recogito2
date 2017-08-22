require.config({
  baseUrl: "/assets/javascripts/",
  fileExclusionRegExp: /^lib$/,
  paths: { marked: '/webjars/marked/0.3.6/marked.min' }
});

require([
  'common/config',
  'document/annotation/common/baseTextApp'
], function(Config, BaseTextApp) {

  // TODO inherit (almost) everything from the Text UI

  jQuery(document).ready(function() {
    var teiURL = jsRoutes.controllers.document.DocumentController
          .getRaw(Config.documentId, Config.partSequenceNo).absoluteURL(),

        CETEIcean = new CETEI();

    CETEIcean.getHTML5(teiURL).then(function(data) {
      document.getElementById("content").appendChild(data);
      new BaseTextApp();
    });
  });

});
