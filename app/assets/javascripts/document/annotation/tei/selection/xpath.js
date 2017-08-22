define([], function() {

  return {

    getXPath : function(node, path) {
      var sibling, count;

      path = path || [];
      if(node.parentNode) {
        path = this.getXPath(node.parentNode, path);
      }

      if(node.previousSibling) {
        count = 1;
        sibling = node.previousSibling;
        do {
          if(sibling.nodeType == 1 && sibling.nodeName == node.nodeName) {count++;}
          sibling = sibling.previousSibling;
        } while(sibling);
        if(count == 1) {count = null;}
      } else if(node.nextSibling) {
        sibling = node.nextSibling;
        do {
          if(sibling.nodeType == 1 && sibling.nodeName == node.nodeName) {
            count = 1;
            sibling = null;
          } else {
            count = null;
            sibling = sibling.previousSibling;
          }
        } while(sibling);
      }

      if(node.nodeType == 1) {
        path.push(node.nodeName.toLowerCase() + (node.id ? "[@id='"+node.id+"']" : count > 0 ? "["+count+"]" : ''));
      }
      return path;
    }

  };

});
