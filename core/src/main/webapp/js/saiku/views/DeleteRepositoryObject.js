/*  
 *   Copyright 2012 OSBI Ltd
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
 

/**
 * fixme by vate 仓库界面：文件删除
 * The delete query confirmation dialog
 */
var DeleteRepositoryObject = Modal.extend({
    type: "delete",
    
    buttons: [
        { text: "确定", method: "del" },
        { text: "取消", method: "close" }
    ],
    
    initialize: function(args) {
        this.options.title = "Confirm deletion";
        this.allQuerys = args.allQuerys;
        this.query = args.query;
        this.success = args.success;
        this.message = '<span class="i18n">Are you sure you want to delete </span>'+'<span>' + this.query.get('name') + '?</span>';
    },

    del: function() {
        this.query.set("id", _.uniqueId("query_"));
		this.query.id = _.uniqueId("query_");
		var that = this;
		try {
			if (this.query.attributes.type == "FOLDER") {
				var childs = new Array();
				for (path in this.allQuerys) {
					if (path.startsWith(that.query.get('file'))) {
						childs.push(path);
					}
				}
				this.query.url = this.query.url() + "?file=" + encodeURIComponent(this.query.get('file')) + "&objType=folder";
			}else {
				this.query.url = this.query.url() + "?file=" + encodeURIComponent(this.query.get('file')) + "&objType=file";
			}
		}catch (e) {
		}

		var layerLoadingIndex = layer.msg("正在删除，请稍后...",{time:10000*1000, icon: 16,shade: 0.01});
		var self = this;
		$.ajax({
			url: Settings.REST_URL + this.query.url,
			type: 'DELETE',
			data: {
				childs:childs == null? null:childs.join(",")
			},
			success: function (data) {
				layer.close(layerLoadingIndex);
				self.success(data);
			},
			error: function (data) {
				layer.close(layerLoadingIndex);
				self.error(data)
			}
		});
        this.close();
    },
	error: function(data) {
		openLayerConfirmDialog(data.responseText);
	}
});
