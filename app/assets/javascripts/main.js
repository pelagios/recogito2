require.config({
  baseUrl : "/assets/javascripts",
  fileExclusionRegExp : /^lib$/,
  paths: { marked: '../../../web-modules/main/webjars/lib/marked/marked.min' },
  modules : [
    { name : 'admin/dashboard' },
    { name : 'admin/maintenance' },
    { name : 'admin/users' },
    { name : 'document/annotation/image/app' },
    { name : 'document/annotation/table/app' },
    { name : 'document/annotation/tei/app' },
    { name : 'document/annotation/text/app' },
    { name : 'document/downloads/app' },
    { name : 'document/map/app' },
    { name : 'document/settings/delete' },
    { name : 'document/settings/metadata' },
    { name : 'document/settings/history' },
    { name : 'document/settings/sharing' },
    { name : 'help/tutorial'},
    { name : 'landing/index' },
    { name : 'my/settings/account' },
    { name : 'my/upload/step1' },
    { name : 'my/upload/step2' },
    { name : 'my/upload/step3' },
    { name : 'my/index' }
 ]
});
