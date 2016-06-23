require.config({
  baseUrl : "/assets/javascripts",
  fileExclusionRegExp : /^lib$/,
  modules : [
    { name : 'document/annotation/text/app' },
    { name : 'document/map/app' },
    { name : 'document/settings/history' },
    { name : 'document/settings/sharing' },
    { name : 'my/upload/step1' },
    { name : 'my/upload/step2' },
    { name : 'my/upload/step3' },
    { name : 'my/index' }
 ]
});
