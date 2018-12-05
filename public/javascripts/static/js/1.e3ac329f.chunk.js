(window.webpackJsonp=window.webpackJsonp||[]).push([[1],{115:function(e,t,n){"use strict";n.d(t,"a",function(){return p});var a=n(4),s=n(5),i=n(7),r=n(6),o=n(8),c=n(1),l=n.n(c),u=n(255).a.div({open:{bottom:"30px",transition:{ease:"easeOut",duration:200}},closed:{bottom:"-140px"}}),p=function(e){function t(e){var n;return Object(a.a)(this,t),(n=Object(i.a)(this,Object(r.a)(t).call(this,e))).state={visible:!1},n}return Object(o.a)(t,e),Object(s.a)(t,[{key:"componentDidMount",value:function(){this.setState({visible:!0})}},{key:"render",value:function(){return l.a.createElement("div",{className:"dropzone-decoration"},l.a.createElement(u,{className:"inner",pose:this.state.visible?"open":"closed"},l.a.createElement("span",{className:"icon"},"\uf0ee"),l.a.createElement("p",{className:"instructions"},"Drop files or IIIF manifest URLs to add them to your workspace."),l.a.createElement("p",{className:"supported"},"Supported formats: plain text (UTF-8), TEI/XML, image files, CSV (UTF-8)")))}}]),t}(c.Component)},116:function(e,t,n){"use strict";n.d(t,"a",function(){return m});var a=n(4),s=n(5),i=n(7),r=n(6),o=n(8),c=n(1),l=n.n(c),u=n(117),p=n.n(u),m=function(e){function t(){return Object(a.a)(this,t),Object(i.a)(this,Object(r.a)(t).apply(this,arguments))}return Object(o.a)(t,e),Object(s.a)(t,[{key:"render",value:function(){return l.a.createElement("div",{className:"clicktrap ".concat(this.props.className)},l.a.createElement("div",{className:"modal-wrapper"},l.a.createElement(p.a,{handle:".modal-header"},l.a.createElement("div",{className:"modal"},l.a.createElement("div",{className:"modal-header"},l.a.createElement("h1",{className:"title"},this.props.title),l.a.createElement("button",{className:"close nostyle",onClick:this.props.onClose},"\ue897")),l.a.createElement("div",{className:"modal-body"},this.props.children)))))}}]),t}(c.Component)},118:function(e,t,n){"use strict";n.d(t,"a",function(){return p});var a=n(4),s=n(5),i=n(7),r=n(6),o=n(8),c=n(1),l=n.n(c),u={MY_DOCUMENTS:"My Documents",SHARED_WITH_ME:"Shared with me"},p=function(e){function t(){return Object(a.a)(this,t),Object(i.a)(this,Object(r.a)(t).apply(this,arguments))}return Object(o.a)(t,e),Object(s.a)(t,[{key:"render",value:function(){var e=this.props.view?u[this.props.view]:this.props.label;return l.a.createElement("div",{className:"breadcrumbs"},l.a.createElement("h2",null,l.a.createElement("a",{className:"root",href:"#"},e),this.props.path&&this.props.path.map(function(e){return l.a.createElement("a",{className:"folder",href:"#".concat(e.id),key:e.id},e.title)})),null!==this.props.count&&l.a.createElement("span",{className:"count"},"(".concat(this.props.count,")")),this.props.children)}}]),t}(c.Component)},119:function(e,t,n){"use strict";n.d(t,"a",function(){return E});var a=n(4),s=n(5),i=n(7),r=n(6),o=n(8),c=n(1),l=n.n(c),u=n(558),p=n(557),m=n(166),h=n(167),d=n(63),f=n.n(d),v={stroke:"#c2c2c2",strokeWidth:1},b={fontSize:"12px",color:"#3f3f3f"},y=function(e){return new Intl.DateTimeFormat("en-GB",{month:"short"}).format(new Date(e))},E=function(e){function t(){return Object(a.a)(this,t),Object(i.a)(this,Object(r.a)(t).apply(this,arguments))}return Object(o.a)(t,e),Object(s.a)(t,[{key:"createEmptySeries",value:function(e,t){var n=new Array(e);return n.fill(0),n.reduce(function(e,t){var n=e[e.length-1];return e.push(n-6048e5),e},[t-6048e5]).reverse().map(function(e){return{timestamp:e,value:0}})}},{key:"padTimeseries",value:function(e){var t=e.length>0?e[0].timestamp:(new Date).getTime();return this.createEmptySeries(19-e.length,t).concat(e)}},{key:"computeChart",value:function(){var e=this.props.stats.over_time.map(function(e){return{timestamp:Date.parse(e.date),value:e.value}});return e.length<20?this.padTimeseries(e):e}},{key:"render",value:function(){return l.a.createElement("div",{className:this.props.className?"activity-widget ".concat(this.props.className):"activity-widget"},l.a.createElement("h2",null,"Activity",this.props.stats&&l.a.createElement("span",{className:"count"},l.a.createElement(f.a,{displayType:"text",value:this.props.stats.total_contributions,thousandSeparator:!0})," edits")),this.props.stats&&l.a.createElement(u.a,{width:this.props.width,height:this.props.height,barCategoryGap:1.5,data:this.computeChart()},l.a.createElement(p.a,{strokeDasharray:"3",horizontal:!1}),l.a.createElement(m.a,{type:"number",dataKey:"timestamp",axisLine:v,tick:b,tickSize:4,tickCount:6,tickLine:v,tickFormatter:y,height:20,domain:["dataMin","dataMax"],padding:{left:5,right:5}}),l.a.createElement(h.a,{dataKey:"value",fill:this.props.fill})))}}]),t}(c.Component)},124:function(e,t,n){"use strict";n.d(t,"a",function(){return h});var a=n(4),s=n(5),i=n(7),r=n(6),o=n(8),c=n(1),l=n.n(c),u=n(20),p=n.n(u),m=n(125),h=function(e){function t(e){var n;return Object(a.a)(this,t),(n=Object(i.a)(this,Object(r.a)(t).call(this,e))).state={collaborators:[]},n.fetchCollaborators(e),n}return Object(o.a)(t,e),Object(s.a)(t,[{key:"componentWillReceiveProps",value:function(e){this.fetchCollaborators(e)}},{key:"fetchCollaborators",value:function(e){var t=this;e.username&&p.a.get("/api/account/".concat(e.username,"/collaborators")).then(function(e){return t.setState({collaborators:e.data})})}},{key:"render",value:function(){return l.a.createElement(l.a.Fragment,null,this.state.collaborators.length>0&&l.a.createElement("div",{className:this.props.className?"collaborator-list ".concat(this.props.className):"collaborator-list"},l.a.createElement("h2",null,this.props.title),l.a.createElement("ul",null,this.state.collaborators.map(function(e){return l.a.createElement("li",{key:e.username},l.a.createElement("a",{href:"/".concat(e.username),title:e.username},l.a.createElement(m.a,{username:e.username})))}))))}}]),t}(c.Component)},125:function(e,t,n){"use strict";n.d(t,"a",function(){return u});var a=n(4),s=n(5),i=n(7),r=n(6),o=n(8),c=n(1),l=n.n(c),u=function(e){function t(){return Object(a.a)(this,t),Object(i.a)(this,Object(r.a)(t).apply(this,arguments))}return Object(o.a)(t,e),Object(s.a)(t,[{key:"stringToHslColor",value:function(e){for(var t=0,n=0;n<e.length;n++)t=e.charCodeAt(n)+((t<<5)-t);return t%360}},{key:"render",value:function(){var e=this.props.username?"hsl(".concat(this.stringToHslColor(this.props.username),", 35%, 65%)"):"#e2e2e2";return l.a.createElement("div",{className:"avatar",style:{backgroundColor:e}},l.a.createElement("div",{className:"inner"},this.props.username&&this.props.username.charAt(0).toUpperCase()))}}]),t}(c.Component)},126:function(e,t,n){"use strict";n.d(t,"a",function(){return p});var a=n(4),s=n(5),i=n(7),r=n(6),o=n(8),c=n(1),l=n.n(c),u=n(125),p=function(e){function t(){return Object(a.a)(this,t),Object(i.a)(this,Object(r.a)(t).apply(this,arguments))}return Object(o.a)(t,e),Object(s.a)(t,[{key:"formatURL",value:function(e){return e.replace(/^https?:\/\//i,"")}},{key:"render",value:function(){var e=this.props.account&&this.props.account.username&&this.props.account.member_since;return l.a.createElement("div",{className:"identity"},l.a.createElement("div",{className:"user"},l.a.createElement(u.a,{username:e&&this.props.account.username}),l.a.createElement("h1",null,e?this.props.account.real_name?this.props.account.real_name:this.props.account.username:l.a.createElement("span",{className:"placeholder"})),l.a.createElement("p",{className:"member-since"},e?l.a.createElement(l.a.Fragment,null,"Joined on ",new Intl.DateTimeFormat("en-GB",{year:"numeric",month:"short",day:"2-digit"}).format(new Date(this.props.account.member_since))):l.a.createElement("span",{className:"placeholder"}))),l.a.createElement("div",{className:"user-extended"},e&&this.props.account.bio&&l.a.createElement("p",{className:"bio"},this.props.account.bio),e&&this.props.account.website&&l.a.createElement("p",{className:"homepage"},l.a.createElement("a",{href:this.props.account.website},this.formatURL(this.props.account.website)))))}}]),t}(c.Component)},127:function(e,t,n){"use strict";var a=n(4),s=n(5),i=n(7),r=n(6),o=n(8),c=n(1),l=n.n(c),u=n(77),p=n(129),m=n(27),h=n(115),d=n(44),f=n(75),v=n(90),b=function(e){function t(e){var n;return Object(a.a)(this,t),(n=Object(i.a)(this,Object(r.a)(t).call(this,e))).updateTotalRowSpan(e),n}return Object(o.a)(t,e),Object(s.a)(t,[{key:"componentWillReceiveProps",value:function(e){this.updateTotalRowSpan(e)}},{key:"updateTotalRowSpan",value:function(e){this._totalSpan=e.columns.reduce(function(e,t){return e+m.b.getSpan(t)},0)}},{key:"getWidth",value:function(e){return m.b.getSpan(e)/this._totalSpan}}]),t}(c.Component),y=function(e){function t(){return Object(a.a)(this,t),Object(i.a)(this,Object(r.a)(t).apply(this,arguments))}return Object(o.a)(t,e),Object(s.a)(t,[{key:"render",value:function(){var e=this;return l.a.createElement("div",{className:"column-labels"},this.props.columns.map(function(n){return l.a.createElement("span",{key:n,style:{width:"".concat(100*Object(v.a)(Object(r.a)(t.prototype),"getWidth",e).call(e,n),"%")},className:"label ".concat(n),onClick:e.props.onSort.bind(e,n)},l.a.createElement("span",{className:"inner-wrapper"},l.a.createElement("span",{className:"inner"},m.c[n]||n),e.props.sortColumn===n&&l.a.createElement("span",{className:"sort icon"},l.a.createElement("span",{className:"inner"},e.props.sortAsc?"\ue688":"\ue682"))))}))}}]),t}(b),E=n(63),k=n.n(E),g=n(243),O=n.n(g),C={TEXT_PLAIN:"icon_text.png",TEXT_TEIXML:"icon_tei.png",IMAGE_UPLOAD:"icon_image.png",IMAGE_IIIF:"icon_iiif.png",DATA_CSV:"icon_csv.png"},N={language:function(e){return e.toUpperCase()},uploaded_at:function(e){return new Intl.DateTimeFormat("en-GB",{year:"numeric",month:"short",day:"2-digit"}).format(new Date(e))},last_edit_at:function(e){return l.a.createElement(O.a,{date:e})},public_visibility:function(e){return"PUBLIC"===e?l.a.createElement("span",{className:"icon",title:"Open to anyone"},"\uf09c"):"WITH_LINK"===e?l.a.createElement("span",{className:"icon",title:"Open to anyone with the link"},"\uf0c1"):l.a.createElement("span",{className:"icon",title:"Private"},"\uf023")},access_level:function(e){return"ADMIN"===e?l.a.createElement("span",{className:"icon",title:"ADMIN"},"\uf0ad"):"WRITE"===e?l.a.createElement("span",{className:"icon",title:"WRITE"},"\uf040"):l.a.createElement("span",{className:"icon",title:"READ"},"\uf06e")},annotations:function(e){return l.a.createElement(k.a,{displayType:"text",value:e,thousandSeparator:!0})},status_ratio:function(e){var t=e.verified+e.unverified+e.not_identifiable,n=e.verified/t*100,a=e.not_identifiable/t*100,s={verified:"verified",unverified:"unverified",not_identifiable:"flagged"},i=["verified","unverified","not_identifiable"].reduce(function(t,n){var a=e[n];return 0===a?t:"".concat(t," ").concat(a," ").concat(s[n],",")},"").slice(0,-1);return l.a.createElement("span",{className:"bar",title:i},l.a.createElement("span",{className:"verified",style:{width:"".concat(n,"%")}}),l.a.createElement("span",{className:"not_identifiable",style:{width:"".concat(a,"%")}}))}},j={agg_document:function(e){return e.author?"".concat(e.author,", ").concat(e.title):e.title}},w=function(e){function t(){return Object(a.a)(this,t),Object(i.a)(this,Object(r.a)(t).apply(this,arguments))}return Object(o.a)(t,e),Object(s.a)(t,[{key:"createAggregateField",value:function(e,n){return l.a.createElement("a",{key:n,href:e,className:n.substring(4),style:{width:"".concat(100*Object(v.a)(Object(r.a)(t.prototype),"getWidth",this).call(this,n),"%")}},j[n](this.props.item))}},{key:"createField",value:function(e,n){var a=N[n],s=void 0!==this.props.item[n]?a?a(this.props.item[n]):this.props.item[n]:"";return l.a.createElement("a",{key:n,href:e,className:n,style:{width:"".concat(100*Object(v.a)(Object(r.a)(t.prototype),"getWidth",this).call(this,n),"%")}},s)}},{key:"render",value:function(){var e=this,t=this.props.item.filetypes[0],n="document/".concat(this.props.item.id,"/part/1/edit");return l.a.createElement("div",{className:"row".concat(this.props.selected?" selected":""),style:this.props.style,onClick:this.props.onClick,onContextMenu:this.props.onContextMenu},this.props.columns.map(function(t){return t.startsWith("agg_")?e.createAggregateField(n,t):e.createField(n,t)}),l.a.createElement("span",{className:"type icon ".concat(t)},l.a.createElement("img",{src:"/assets/images/".concat(C[t]),alt:"icon type ".concat(t)})))}}]),t}(b),_=function(e){function t(e){var n;return Object(a.a)(this,t),(n=Object(i.a)(this,Object(r.a)(t).call(this,e))).state={editable:!1,title:n.props.item.title},n}return Object(o.a)(t,e),Object(s.a)(t,[{key:"componentWillReceiveProps",value:function(e){this.setState({editable:!1,title:e.item.title})}},{key:"makeEditable",value:function(e){var t=this;this.setState({editable:!0},function(){return t._input.select()}),e.preventDefault(),e.stopPropagation()}},{key:"onChange",value:function(e){this.setState({title:e.target.value})}},{key:"onKeyPress",value:function(e){var t=this;13===e.which&&this.setState({editable:!1},function(){return t.props.onRename(t.props.item,t.state.title)})}},{key:"render",value:function(){var e=this;return l.a.createElement("a",{href:"#".concat(this.props.item.id),style:this.props.style,className:"row folder".concat(this.props.selected?" selected":""),onClick:this.props.onClick,onContextMenu:this.props.onContextMenu},l.a.createElement("span",{className:"folder-title"},this.state.editable?l.a.createElement("input",{ref:function(t){return e._input=t},value:this.state.title,onClick:this.makeEditable.bind(this),onChange:this.onChange.bind(this),onKeyDown:this.onKeyPress.bind(this)}):l.a.createElement("span",{className:"editable",onClick:this.makeEditable.bind(this)},this.state.title)),l.a.createElement("span",{className:"type icon"},"\uf07b"))}}]),t}(c.Component),S=n(85),D=n(116),M=Object(S.SortableElement)(function(e){var t=e.label;return l.a.createElement("div",{className:"card"},t)}),x=Object(S.SortableContainer)(function(e){var t=e.items;return l.a.createElement("div",{className:"column-order"},t.map(function(e,t){return l.a.createElement(M,{key:"card-".concat(t),index:t,label:m.c[e]})}))}),R=function(e){function t(){return Object(a.a)(this,t),Object(i.a)(this,Object(r.a)(t).apply(this,arguments))}return Object(o.a)(t,e),Object(s.a)(t,[{key:"render",value:function(){var e=this;return l.a.createElement(x,{items:this.props.items,onSortEnd:function(t){return e.props.onSortEnd(t.oldIndex,t.newIndex)}})}}]),t}(c.Component),I=function(e){function t(e){var n;return Object(a.a)(this,t),(n=Object(i.a)(this,Object(r.a)(t).call(this,e))).state={columns:e.columns.slice(0)},n}return Object(o.a)(t,e),Object(s.a)(t,[{key:"componentWillReceiveProps",value:function(e){e.columns!==this.props.columns&&this.setState({columns:e.columns.slice(0)})}},{key:"getCheckboxStates",value:function(e){var t=this,n={};return m.a.forEach(function(e){var a=t.state.columns.includes(e);n[e]=a}),n}},{key:"setAllRows",value:function(e){var t=this;e?function(){var e=m.a.filter(function(e){return!t.state.columns.includes(e)});t.setState({columns:t.state.columns.concat(e)})}():t.setState({columns:[]})}},{key:"onClickAll",value:function(){m.a.length===this.state.columns.length?this.setAllRows(!1):this.setAllRows(!0)}},{key:"toggleOne",value:function(e){this.setState(function(t){var n=t.columns.indexOf(e);return n<0?t.columns.push(e):t.columns.splice(n,1),t})}},{key:"onSort",value:function(e,t){this.setState({columns:Object(S.arrayMove)(this.state.columns,e,t)})}},{key:"render",value:function(){var e=this;return l.a.createElement(D.a,{className:"preferences",title:"Configure Columns",onClose:this.props.onCancel},l.a.createElement("div",{className:"scroll-pane"},l.a.createElement("div",{className:"selected-columns"},l.a.createElement("button",{className:"all nostyle",onClick:this.onClickAll.bind(this)},"All"),l.a.createElement("ul",null,m.a.map(function(t){return l.a.createElement("li",{key:t},l.a.createElement("input",{type:"checkbox",id:t,name:t,checked:e.state.columns.includes(t),onChange:e.toggleOne.bind(e,t)}),l.a.createElement("label",{htmlFor:t},m.c[t]))}))),l.a.createElement(R,{items:this.state.columns,onSortEnd:this.onSort.bind(this)})),l.a.createElement("div",{className:"buttons"},l.a.createElement("button",{className:"btn",onClick:this.props.onSave.bind(this,this.state.columns)},"Save"),l.a.createElement("button",{className:"btn outline",onClick:this.props.onCancel},"Cancel")))}}]),t}(c.Component);n.d(t,"a",function(){return A});var A=function(e){function t(e){var n;return Object(a.a)(this,t),(n=Object(i.a)(this,Object(r.a)(t).call(this,e))).state={prefsOpen:!1,columns:m.b.filterByView(e.columns,e.view),selection:new f.a(e.folders.concat(e.documents),e.selection)},n}return Object(o.a)(t,e),Object(s.a)(t,[{key:"componentWillReceiveProps",value:function(e){this.setState({columns:m.b.filterByView(e.columns,e.view),selection:new f.a(e.folders.concat(e.documents),e.selection)})}},{key:"onClick",value:function(e,t,n){var a=e.getModifierState("Shift"),s=e.getModifierState("Control");(a||s||!this.props.selection.includes(t))&&(a?this.state.selection.selectRange(n):this.state.selection.selectItem(t,s),this.props.onSelect(this.state.selection.getSelectedItems()),e.preventDefault())}},{key:"onContextMenu",value:function(e,t,n){e.preventDefault()}},{key:"rowRenderer",value:function(){var e=this,t=this.props.folders.concat(this.props.documents);return function(n){var a=t[n.index],s=e.props.selection&&e.props.selection.includes(a);return"FOLDER"===a.type?l.a.createElement(_,{key:n.key,style:n.style,item:a,selected:s,onClick:function(t){return e.onClick(t,a,n.index)},onContextMenu:function(t){return e.onContextMenu(t,a,n.index)},onRename:e.props.onRenameFolder}):l.a.createElement(w,{key:n.key,style:n.style,columns:e.state.columns,item:a,selected:s,onClick:function(t){return e.onClick(t,a,n.index)},onContextMenu:function(t){return e.onContextMenu(t,a,n.index)}})}}},{key:"showPreferences",value:function(e){this.setState({prefsOpen:e})}},{key:"onSavePreferences",value:function(e){this.setState({prefsOpen:!1}),this.props.onChangeColumnPrefs(e)}},{key:"sortBy",value:function(e){var t=!this.props.sorting||(this.props.sorting.by!==e||!this.props.sorting.asc);this.props.onSort({by:e,asc:t})}},{key:"onDrag",value:function(e){this.setState({drag:e})}},{key:"onDrop",value:function(e,t,n){var a=n.dataTransfer.getData("URL"),s=a?function(){var e=document.createElement("a");return e.href=a,e.hostname}():null;this.setState({drag:!1}),e.length>0?this.props.onDropFiles(e):a&&s!==window.location.hostname&&this.props.onDropURL(a)}},{key:"render",value:function(){var e=this,t=l.a.Children.toArray(this.props.children).filter(function(e){return e.type===d.a}).shift(),n=l.a.createElement("div",{className:"documents-pane table-pane"},l.a.createElement(u.a,null,function(t){var n=t.height,a=t.width;return l.a.createElement(u.b,{className:"virtualized-list",width:a,height:n,rowCount:e.props.folders.length+e.props.documents.length,rowHeight:47,rowRenderer:e.rowRenderer()})}),this.state.drag&&l.a.createElement(h.a,null),this.props.busy&&l.a.createElement("div",{className:"busy-mask"}));return l.a.createElement(l.a.Fragment,null,t,l.a.createElement("div",{className:"documents-table-header"},l.a.createElement(y,{columns:this.state.columns,onSort:this.sortBy.bind(this),sortColumn:this.props.sorting?this.props.sorting.by:null,sortAsc:this.props.sorting?this.props.sorting.asc:null}),l.a.createElement("button",{className:"column-options-btn nostyle icon",onClick:this.showPreferences.bind(this,!0)},"\uf141")),this.props.disableFiledrop?n:l.a.createElement(p.a,{className:"dropzone",disableClick:!0,onDragEnter:this.onDrag.bind(this,!0),onDragLeave:this.onDrag.bind(this,!1),onDrop:this.onDrop.bind(this)},n),this.state.prefsOpen&&l.a.createElement(I,{columns:this.props.columns,onCancel:this.showPreferences.bind(this,!1),onSave:this.onSavePreferences.bind(this)}))}}]),t}(c.Component)},128:function(e,t,n){"use strict";var a=n(4),s=n(5),i=n(7),r=n(6),o=n(8),c=n(1),l=n.n(c),u=n(129),p=n(77),m=n(75),h=function(e){function t(){return Object(a.a)(this,t),Object(i.a)(this,Object(r.a)(t).apply(this,arguments))}return Object(o.a)(t,e),Object(s.a)(t,[{key:"render",value:function(){return l.a.createElement("div",{className:"cell"},l.a.createElement("div",{className:"item-wrapper"},l.a.createElement("a",{href:"#",className:"folder"},l.a.createElement("div",{className:"label"},this.props.title))))}}]),t}(c.Component),d=function(e){function t(){return Object(a.a)(this,t),Object(i.a)(this,Object(r.a)(t).apply(this,arguments))}return Object(o.a)(t,e),Object(s.a)(t,[{key:"render",value:function(){var e=this.props.filetypes[0],t=this.props.fileCount>1;return l.a.createElement("div",{className:"cell".concat(this.props.selected?" selected":""),onClick:this.props.onClick,onDoubleClick:this.props.onDoubleClick},l.a.createElement("div",{className:"inner"},l.a.createElement("div",{className:"item-wrapper".concat(t?" stacked":"")},t&&l.a.createElement("div",{className:"stack"}),l.a.createElement("a",{href:"document/".concat(this.props.id,"/part/1/edit"),className:"document ".concat(e)},l.a.createElement("div",{className:"label"},this.props.title)))))}}]),t}(c.Component),f=n(44),v=n(115);n.d(t,"a",function(){return b});var b=function(e){function t(e){var n;return Object(a.a)(this,t),(n=Object(i.a)(this,Object(r.a)(t).call(this,e))).state={selection:new m.a(e.folders.concat(e.documents),e.selection)},n}return Object(o.a)(t,e),Object(s.a)(t,[{key:"componentWillReceiveProps",value:function(e){this.setState({selection:new m.a(e.folders.concat(e.documents),e.selection)})}},{key:"onClick",value:function(e,t,n){var a=e.getModifierState("Shift"),s=e.getModifierState("Control");a?this.state.selection.selectRange(n):this.state.selection.selectItem(t,s),this.props.onSelect(this.state.selection.getSelectedItems()),e.preventDefault()}},{key:"onDoubleClick",value:function(e){window.location.href="document/".concat(e.id,"/part/1/edit")}},{key:"rowRenderer",value:function(e,t){var n=this,a=this.props.folders.concat(this.props.documents);return function(t){var s=t.index*e,i=Math.min(s+e,a.length)-s,r=new Array(i).fill(void 0).map(function(e,i){var r=i+s,o=a[r];return"FOLDER"===o.type?l.a.createElement(h,{key:r,title:o.title}):l.a.createElement(d,{key:r,id:o.id,title:o.title,filetypes:o.filetypes,fileCount:o.file_count,selected:n.props.selection&&n.props.selection.includes(o),onClick:function(e){return n.onClick(e,o,t.index)},onDoubleClick:n.onDoubleClick.bind(n,o)})});return i<e&&r.push(new Array(e-i).fill(void 0).map(function(e,t){return l.a.createElement("div",{className:"cell dummy",key:"dummy-".concat(t)})})),l.a.createElement("div",{key:t.key,style:t.style,className:"row"},r)}}},{key:"onDrag",value:function(e){this.setState({drag:e})}},{key:"onDrop",value:function(e,t,n){var a=n.dataTransfer.getData("URL");this.setState({drag:!1}),e.length>0?this.props.onDropFiles(e):a&&this.props.onDropURL(a)}},{key:"render",value:function(){var e=this,t=l.a.Children.toArray(this.props.children).filter(function(e){return e.type===f.a}).shift(),n=l.a.createElement(p.a,null,function(t){var n=t.height,a=t.width,s=e.props.folders.length+e.props.documents.length,i=Math.floor(a/192),r=Math.ceil(s/i);return l.a.createElement(p.b,{className:"virtualized-grid",width:a,height:n,rowCount:r,rowHeight:192,rowRenderer:e.rowRenderer(i,r)})});return l.a.createElement(l.a.Fragment,null,t,l.a.createElement("div",{className:"documents-pane grid-pane"},this.props.disableFiledrop?n:l.a.createElement(u.a,{className:"dropzone",disableClick:!0,onDragEnter:this.onDrag.bind(this,!0),onDragLeave:this.onDrag.bind(this,!1),onDrop:this.onDrop.bind(this)},n),this.state.drag&&l.a.createElement(v.a,null)))}}]),t}(c.Component)},27:function(e,t,n){"use strict";n.d(t,"a",function(){return r}),n.d(t,"c",function(){return o}),n.d(t,"b",function(){return u});var a=n(4),s=n(5),i=["owner","shared_by","access_level"],r=["author","title","language","date_freeform","uploaded_at","last_edit_at","last_edit_by","annotations","public_visibility","status_ratio"].concat(i),o={author:"Author",title:"Title",language:"Language",date_freeform:"Date",uploaded_at:"Uploaded at",last_edit_at:"Last edit",last_edit_by:"Last edit by",annotations:"Annotations",public_visibility:"Visibility",status_ratio:"Verification ratio",activity:"Activity graph",owner:"Document owner",shared_by:"Shared by",access_level:"Access"},c={agg_document:"XL",author:"M",title:"L",language:"M",date_freeform:"M",uploaded_at:"M",last_edit_at:"M",last_edit_by:"M",annotations:"M",public_visibility:"M",status_ratio:"M",activity:"M",owner:"M",shared_by:"M",access_level:"M"},l={agg_document:["author","document"]},u=function(){function e(){Object(a.a)(this,e)}return Object(s.a)(e,null,[{key:"getSpan",value:function(e){var t=c[e];return"XL"===t?6:"L"===t?4:"M"===t?2:"S"===t?1:void 0}},{key:"expandAggregatedColumns",value:function(e){return e.reduce(function(e,t){return t.startsWith("agg_")?e=e.concat(l[t]):e.push(t),e},[])}},{key:"filterByView",value:function(e,t){return"MY_DOCUMENTS"===t?e.filter(function(e){return!i.includes(e)}):e}}]),e}()},41:function(e,t,n){"use strict";n.d(t,"a",function(){return i});var a=n(4),s=n(5),i=function(){function e(){Object(a.a)(this,e)}return Object(s.a)(e,null,[{key:"load",value:function(){var e={};return["view","presentation","table_columns","table_sorting"].map(function(t){var n=localStorage.getItem("r2.workspace.".concat(t));n&&(e[t]=JSON.parse(n))}),e}},{key:"save",value:function(e,t){var n="r2.workspace.".concat(e);localStorage.setItem(n,JSON.stringify(t))}}]),e}()},44:function(e,t,n){"use strict";n.d(t,"a",function(){return f});var a=n(4),s=n(5),i=n(7),r=n(6),o=n(8),c=n(23),l=n(1),u=n.n(l),p=n(234),m=n.n(p),h=n(235),d={editing:!1,modifiedContent:null},f=function(e){function t(e){var n;return Object(a.a)(this,t),(n=Object(i.a)(this,Object(r.a)(t).call(this,e))).state={},n.onKeydown=n.onKeydown.bind(Object(c.a)(Object(c.a)(n))),n}return Object(o.a)(t,e),Object(s.a)(t,[{key:"onKeydown",value:function(e){27===e.which?this.onCancel():e.ctrlKey&&13===e.which&&this.onSave()}},{key:"onEdit",value:function(){document.addEventListener("keydown",this.onKeydown,!1),this.setState({editing:!0})}},{key:"onChange",value:function(e,t){this.setState({modifiedContent:t})}},{key:"onDelete",value:function(){document.removeEventListener("keydown",this.onKeydown,!1),this.props.onDelete&&this.props.onDelete()}},{key:"onSave",value:function(){document.removeEventListener("keydown",this.onKeydown,!1),this.props.onUpdate&&this.props.onUpdate(this.state.modifiedContent),this.setState(d)}},{key:"onCancel",value:function(){document.removeEventListener("keydown",this.onKeydown,!1),"boolean"===typeof this.props.content?this.onDelete():this.setState(d)}},{key:"renderView",value:function(e){return u.a.createElement("div",{className:"readme"},u.a.createElement("div",{className:"wrapper"},u.a.createElement("div",{className:"textbox"},u.a.createElement(m.a,{source:e})),this.props.onUpdate&&this.props.onDelete&&u.a.createElement("span",{className:"buttons modify"},u.a.createElement("button",{className:"icon nostyle",title:"Edit",onClick:this.onEdit.bind(this)},"\uf040"))))}},{key:"renderEdit",value:function(e){return u.a.createElement("div",{className:"readme editing"},u.a.createElement("div",{className:"wrapper"},u.a.createElement(h.a,{tagName:"div",className:"textbox",content:e,editable:!0,multiLine:!0,onChange:this.onChange.bind(this)}),u.a.createElement("div",{className:"editbar"},u.a.createElement("span",{className:"hint"},"Supports styling with ",u.a.createElement("a",{href:"#"},"Markdown")),u.a.createElement("span",{className:"buttons"},u.a.createElement("button",{className:"label nostyle",onClick:this.onDelete.bind(this)},"Delete description"),u.a.createElement("button",{className:"icon nostyle",onClick:this.onSave.bind(this)},"\uf00c"),u.a.createElement("button",{className:"icon nostyle",onClick:this.onCancel.bind(this)},"\uf00d")))))}},{key:"render",value:function(){var e="boolean"===typeof this.props.content;e&&document.addEventListener("keydown",this.onKeydown,!1);var t=e?"":this.state.modifiedContent?this.state.modifiedContent:this.props.content;return this.state.editing||e?this.renderEdit(t):this.renderView(t)}}]),t}(l.Component)},75:function(e,t,n){"use strict";n.d(t,"a",function(){return i});var a=n(4),s=n(5),i=function(){function e(t,n){Object(a.a)(this,e),this._allItems=t,this._selection=n}return Object(s.a)(e,[{key:"clear",value:function(){this._selection=[]}},{key:"isEmpty",value:function(){return 0===this._selection.length}},{key:"getSelectedItems",value:function(){return this._selection}},{key:"selectItem",value:function(e,t){var n=this._selection.indexOf(e);t?n>-1?this._selection.splice(n,1):this._selection.push(e):this._selection=[e]}},{key:"selectRange",value:function(e){var t=this,n=this._selection.map(function(e){return t._allItems.indexOf(e)}),a=Math.min.apply(null,n),s=Math.max.apply(null,n),i=e>s||e<a,r=function(e,n){return t._allItems.slice(e,n+1)};this._selection=i&&e>s?r(a,e):i?r(e,s):r(a,e)}}]),e}()},86:function(e,t,n){"use strict";n.d(t,"a",function(){return u});var a=n(4),s=n(5),i=n(7),r=n(6),o=n(8),c=n(1),l=n.n(c),u=function(e){function t(){return Object(a.a)(this,t),Object(i.a)(this,Object(r.a)(t).apply(this,arguments))}return Object(o.a)(t,e),Object(s.a)(t,[{key:"render",value:function(){return l.a.createElement("div",{className:"header-icon ".concat(this.props.className),onClick:this.props.onClick},this.props.link?l.a.createElement("a",{href:this.props.link,className:"icon inner"},this.props.icon):l.a.createElement("span",{className:"icon inner"},this.props.icon))}}]),t}(c.Component)}}]);
//# sourceMappingURL=1.e3ac329f.chunk.js.map