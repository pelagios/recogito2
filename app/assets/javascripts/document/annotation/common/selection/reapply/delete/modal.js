define([], function() {

  var BUTTONS =


      getMessage = function(toDelete, quote) {
        var message;

        if (toDelete == 1)
          message = 'There is 1 more annotated occurrence';
        else
          message = 'There are ' + toDelete + ' more annotated occurrences';

        message += ' of <span class="quote">' + quote + '</span> in the text. Do you want to delete ';

        if (toDelete == 1)
          message += 'it, too?';
        else
          message += 'those, too?';

        return message;
      };

  return {

    prompt : function(quote, toDelete, onDelete) {
      var message = getMessage(toDelete.length, quote),
          element = jQuery(
            '<div class="clicktrap">' +
              '<div class="alert info reapply">' +
                '<h1>Re-Apply</h1>' +
                '<p class="message">' + message + '</p>' +
                '<p class="buttons">' +
                  '<span class="stacked">' +
                    '<button class="btn" data-action="DELETE">YES, delete</button>' +
                    '<button class="btn outline" data-action="CANCEL">NO, keep them</button>' +
                  '</span>' +
                '</p>' +
              '</div>' +
            '</div>').appendTo(document.body);

      element.on('click', 'button', function(evt) {
        var btn = jQuery(evt.target),
            action = btn.data('action');

        if (action == 'DELETE') onDelete();
        element.remove();
      });

    }

  }

});
