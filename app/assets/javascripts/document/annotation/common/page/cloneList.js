define([], function() {

  var CloneList = function(attachedToEl, clones) {

    var self = this,

        element = jQuery(
          '<div class="clone-list">' +
            '<ul>' +
            '</ul>' +
          '</div>').hide(),

        toggle = function() {
          if (element.is(':visible'))
            element.hide();
          else 
            element.show();
        },

        init = function() {
          var ul = element.find('ul'),
              trigger = jQuery(attachedToEl);

          clones.forEach(function(clone) {
            ul.append(jQuery(
              '<li><a target="_blank" href="/document/' + clone.id + '">' + 
                clone.owner + '/' + clone.id + '</a></li>'
            ));
          }); 

          trigger.append(element);      
          trigger.click(toggle);
        };

    init();
  };

  return CloneList;

});