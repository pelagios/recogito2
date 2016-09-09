define(function() {

  // Cf. http://stackoverflow.com/questions/5186441/javascript-drag-and-drop-for-touch-devices
  var touchTranslation = function(event) {
        var touch = event.changedTouches[0],
            simulatedEvent = document.createEvent('MouseEvent'),
            type;
            
        // Translate event types
        if (event.type === 'touchstart')
          type = 'mousedown';
        else if (event.type === 'touchmove')
          type = 'mousemove';
        else if ( event.type === 'touchend')
          type = 'mouseup';
        else
          // break
          return;

        simulatedEvent.initMouseEvent(type, true, true, window, 1,
          touch.screenX, touch.screenY, touch.clientX, touch.clientY,
          false, false, false, false, 0, null);

        touch.target.dispatchEvent(simulatedEvent);
      };

  return {

    /**
     * With slight modifications, from:
     * http://stackoverflow.com/questions/13358292/capture-tap-event-with-pure-javascript
     */
    enableTap : function()  {
      jQuery.event.special.tap = {
        setup : function(data, namespaces) {
                  var elem = jQuery(this);
                  elem.bind('touchstart', jQuery.event.special.tap.handler)
                      .bind('touchmove', jQuery.event.special.tap.handler)
                      .bind('touchend', jQuery.event.special.tap.handler);
                },

        teardown : function(namespaces) {
                     var elem = jQuery(this);
                     elem.unbind('touchstart', jQuery.event.special.tap.handler)
                         .unbind('touchmove', jQuery.event.special.tap.handler)
                         .unbind('touchend', jQuery.event.special.tap.handler);
                   },

        handler : function(event) {
                    var elem = jQuery(this);
                    elem.data(event.type, 1);
                    if (event.type === 'touchend' && !elem.data('touchmove')) {
                      event.type = 'tap';
                      jQuery.event.dispatch.apply(this, arguments);
                    } else if (elem.data('touchend')) {
                      elem.removeData('touchstart touchmove touchend');
                    }
                  }
      };
    },

    /**
     * With slight modifications, from:
     * ---
     * jQuery Double Tap
     * Developer: Sergey Margaritov (sergey@margaritov.net)
     * Date: 22.10.2013
     * Based on jquery documentation http://learn.jquery.com/events/event-extensions/
     * ---
     * https://gist.github.com/attenzione/7098476
     */
    enableDoubleTap : function() {
      jQuery.event.special.doubletap = {
        bindType: 'touchend',
        delegateType: 'touchend',

        handle: function(event) {
          var handleObj   = event.handleObj,
              targetData  = jQuery.data(event.target),
              now         = new Date().getTime(),
              delta       = targetData.lastTouch ? now - targetData.lastTouch : 0,
              delay       = 300;

          if (delta < delay && delta > 30) {
            targetData.lastTouch = null;
            event.type = handleObj.origType;
            ['clientX', 'clientY', 'pageX', 'pageY'].forEach(function(property) {
              event[property] = event.originalEvent.changedTouches[0][property];
            });

            // let jQuery handle the triggering of "doubletap" event handlers
            handleObj.handler.apply(this, arguments);
          } else {
            targetData.lastTouch = now;
          }
        }
      };
    },

    enableTouchEvents : function() {
      this.enableTap();
      this.enableDoubleTap();
    },

    makeDraggable : function(element) {
      element[0].addEventListener('touchstart', touchTranslation, true);
      element[0].addEventListener('touchmove', touchTranslation, true);
      element[0].addEventListener('touchend', touchTranslation, true);
      element[0].addEventListener('touchcancel', touchTranslation, true);
    }

  };

});
