require.config({
  baseUrl : "/assets/javascripts",
  fileExclusionRegExp : /^lib$/,
  paths: {
    marked: '../../../web-modules/main/webjars/lib/marked/marked.min',
    i18n: '../../../../../public/vendor/i18n'
  },
  modules : [
    { name : 'document/annotation/image/app' },
    { name : 'document/annotation/table/app' },
    { name : 'document/annotation/tei/app' },
    { name : 'document/annotation/text/app' },
    { name : 'document/downloads/app' },
    { name : 'document/map/app' },
    { name : 'document/settings/delete' },
    { name : 'document/settings/metadata' },
    { name : 'document/settings/preferences' },
    { name : 'document/settings/history' },
    { name : 'document/settings/sharing' },
    { name : 'document/stats/entities' },
    { name : 'document/stats/tags' },
    { name : 'help/tutorial'},
    { name : 'help/articles'},
    { name : 'landing/index' },
    { name : 'my/settings/account' }
  ]
});
