define([], function() {

  var GeoResolutionEditor = function() {
    var element = jQuery(
          '<div class="clicktrap">' +
            '<div class="geores-wrapper">' +
              '<div class="geores-editor">' +
                '<div class="geores-editor-header">' +
                  '<div class="geores-search">' +
                    '<input class="search inline" type="text" placeholder="Search for a Place..." />' +
                    '<button class="search icon">&#xf002;</button>' +
                  '</div>' +
                  '<button class="nostyle flag">&#xe842;</button>' +
                  '<button class="nostyle cancel">&#xe897;</button>' +
                '</div>' +

                '<div class="geores-editor-body">' +
                '</div>' +
              '</div>' +
            '</div>' +
          '</div>'),

          searchInput = element.find('.search'),

          btnCancel = element.find('.cancel'),

          open = function(quote, placeBody) {
            searchInput.val(quote);
            element.show();
          },

          close = function() {
            element.hide();
          };

    jQuery(document.body).append(element);

    btnCancel.click(close);

    this.open = open;
  };

  return GeoResolutionEditor;

});
