define(function() {

  return {

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
    }

  };

});
