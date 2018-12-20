define([
  'common/api',
  'common/config'
], function(API, Config) {

  return {

    init : function() {
      Promise.all([
        API.listAnnotationsInDocument(Config.documentId),
        API.listPlacesInDocument(Config.documentId, 0, 10000)
      ]).then(function(values) {
        var args = {
          annotations: values[0],
          entities: values[1].items
        };
  
        for (var name in window.plugins) {
          if (window.plugins.hasOwnProperty(name))
            window.plugins[name](args);
        }
      });
    }

  };

});
