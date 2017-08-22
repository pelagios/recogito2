require.config({
  baseUrl: "/assets/javascripts/",
  fileExclusionRegExp: /^lib$/,
  paths: { marked: '/webjars/marked/0.3.6/marked.min' }
});

require(['document/annotation/common/baseTextApp'], function(BaseTextApp) {

  /** Start on page load **/
  jQuery(document).ready(function() { new BaseTextApp(); });

});
