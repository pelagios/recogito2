define([], function() {

  var Help = function() {

    var element = (function() {
          var el = jQuery(
                '<div class="help-window panel">' +
                  '<h2>Help' +
                    '<button class="nostyle outline-icon cancel">&#xe897;</button>' +
                  '</h2>' +
                  '<div class="help-body inner">' +

                   // '<h3>Keyboard Shortcuts</h3>' +
                   // '<table class="shortcuts">' +
                   // '</table>' +

                   '<h3>Map Navigation</h3>' +
                   '<table class="navigation">' +
                     '<tr>' +
                       '<td>Hold <span class="key">SHIFT</span></td>' +
                       '<td>to drag a box and zoom to the area</td>' +
                     '</tr>' +

                     '<tr>' +
                       '<td>Hold <span class="key">SHIFT</span> + <span class="key">Alt</span></td>' +
                       '<td>to rotate the map around the center of the screen</td>' +
                     '</tr>' +
                   '</table>' +

                  '</div>' +
                '</div>');

          el.hide();
          el.draggable({ handle: 'h2' });
          jQuery(document.body).append(el);
          return el;
        })(),

        btnClose = element.find('button'),

        setInitialPosition = function() {
          var h = jQuery(window).height(),
              w = jQuery(window).width(),
              top = (h - element.outerHeight()) / 2,
              left = (w - element.outerWidth()) / 2;

          element.css({ left: left, top: top });
        },

        isVisible = function() {
          return element.is(':visible');
        },

        open = function() {
          element.show();
        },

        close = function() {
          element.hide();
        };

    btnClose.click(close);

    setInitialPosition();

    this.open = open;
    this.close = close;
    this.isVisible = isVisible;

  };

  return Help;

});
