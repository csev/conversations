Vue.component('topic-listing', {
  template: `
<div class="conversations-topics-listing">
  <table class="table table-hover table-condensed">
      <thead>
          <tr>
              <th :class="'col-sm-3' + sortClassesForColumn('title')" v-on:click="toggleSort('title')">Topic Title</th>
              <th :class="'col-sm-1' + sortClassesForColumn('type')" v-on:click="toggleSort('type')">Topic Type</th>
              <th class="col-sm-2">Posters</th>
              <th class="col-sm-2">Available To</th>
              <th class="col-sm-1">Posts</th>
              <th :class="'col-sm-2' + sortClassesForColumn('last_activity_at')" v-on:click="toggleSort('last_activity_at')">Last Activity</th>
              <th></th>
          </tr>
      </thead>
      <tbody>
          <tr v-for="topic in topics" @click="handleClick(topic)" v-bind:class="{ 'info': isSelected(topic) }">
            <td>
                <template v-if="!topic.settings.published">
                    <span class="text-muted">[DRAFT]</span>
                </template>
                {{topic.title}}
            </td>
            <td>{{capitalize(topic.type)}}</td>
            <td>
              <!-- FIXME limit to first 6 and add plus number of others -->
              <div v-for="poster in postersToDisplay(topic.posters)" class="topic-poster-photo" :title="buildPosterTooltip(poster)">
                <img :src="buildPosterProfilePicSrc(poster)" :alt="buildPosterProfilePicAlt(poster)" />
              </div>
              <template v-if="topic.posters.length > maxPostersToDisplay">
                <div class="topic-poster-photo topic-poster-others" :title="'Plus ' + (topic.posters.length - maxPostersToDisplay) + ' other posters'">
                  +{{topic.posters.length - maxPostersToDisplay}}
                </div>
              </template>
            </td>
            <td>
              <template v-if="topic.settings.availability == 'GROUPS'">
                  <template v-if="topic.settings.groups.length > 5">
                      <span :title="buildGroupNamesDisplay(topic)">{{topic.settings.groups.length}} Groups</span>
                  </template>
                  <template v-if="topic.settings.groups.length == 0">
                      Groups: <span class="text-muted">None</span>
                  </template>
                  <template v-else>
                    Groups: {{buildGroupNamesDisplay(topic)}}
                  </template>
              </template>
              <template v-else-if="topic.settings.availability == 'ENTIRE_SITE'">
                Entire Site
              </template>
              <template v-else>
                {{topic.settings.availability}}
              </template>
            </td>
            <td>
              {{topic.postCount}}
            </td>
            <td>{{formatEpochTime(topic.lastActivityAt)}}</td>
            <td>
              <a :href="baseurl+'topic?uuid='+topic.uuid" title="View Topic" class="button"><i class="fa fa-external-link"></i></a>
              <template v-if="is_instructor">
                <edit-topic-settings-wrapper :topic="topic" :baseurl="baseurl"></edit-topic-settings-wrapper>
              </template>
            </td>
          </tr>
      </tbody>
  </table>
  <template v-if="topics.length > 0">
      <listing-pagination :baseurl="baseurl"></listing-pagination>
  </template>
</div>
`,
  data: function() {
    return {
      topics: [],
      page: parseInt(this.initial_page),
      order_by: this.initial_order_by,
      order_direction: this.initial_order_direction,
      maxPostersToDisplay: 5,
      count: 0,
      selected: undefined,
      clicks: 0,
      click_timer: undefined,
    };
  },
  props: ['baseurl',
          'initial_order_by',
          'initial_order_direction',
          'initial_page',
          'page_size',
          'is_instructor'],
  methods: {
    isSelected: function(topic) {
      return topic === this.selected;
    },
    loadTopics: function() {
      $.ajax({
        url: this.baseurl+'feed/topics',
        type: 'get',
        data: {page: this.page, order_by: this.order_by, order_direction: this.order_direction},
        dataType: 'json',
        success: (json) => {
          this.count = json.count || 0;
          this.topics = json.topics || [];
        },
      });
    },
    capitalize: function(string) {
      return string.charAt(0).toUpperCase() + string.slice(1);
    },
    formatEpochTime: function(epoch) {
      return new Date(parseInt(epoch)).toLocaleString();
    },
    buildPosterTooltip: function(poster) {
      return poster.firstName + ' ' + poster.lastName + ' last posted at ' + this.formatEpochTime(poster.latestPostAt);
    },
    buildPosterProfilePicAlt(poster) {
      return 'Profile picture for ' + poster.netId;
    },
    buildPosterProfilePicSrc(poster) {
      return '/direct/profile/' + poster.userId + '/image';
    },
    buildGroupNamesDisplay(topic) {
      var names = [];
      topic.settings.groups.forEach(function(groupId) {
        if (topic.settings.group_names[groupId]) {
          names.push(topic.settings.group_names[groupId])
        }
      });
      return names.join(', ')
    },
    toggleSort: function(column) {
      this.page = 0;
      if (this.order_by == column) {
        if (this.order_direction === 'asc') {
          this.order_direction = 'desc';
        } else {
          this.order_direction = 'asc';
        }
      } else {
        this.order_by = column;
        this.order_direction = 'asc';
      }
      this.loadTopics();
    },
    sortClassesForColumn: function(column) {
      const classes = ['conversations-sortable'];
      if (column === this.order_by) {
        const sortDirection = this.order_direction.toLowerCase();
        classes.push('conversations-sortable-active');
        classes.push('conversations-sortable-active-'+sortDirection);
      }
      return ' ' + classes.join(' ');
    },
    postersToDisplay: function(posters) {
      if (posters.length < this.maxPostersToDisplay) {
        return posters;
      } else {
        return posters.slice(0, this.maxPostersToDisplay);
      }
    },
    handleClick: function(topic) {
      this.clicks++;
      if (this.clicks === 1) {
        this.selected = topic;
        this.click_timer = setTimeout(() => {
          this.clicks = 0;
        }, 200);
      } else{
         clearTimeout(this.click_timer);
         location.href = this.baseurl+'topic?uuid='+topic.uuid;
      }
    }
  },
  mounted: function() {
    this.loadTopics();
  },
});


Vue.component('listing-pagination', {
  template: `
<div class="conversations-topics-listing-pagination">
    <template v-if="maxPage() > 0">
      <nav aria-label="Page navigation" class="text-center">
        <ul class="pagination">
          <li v-bind:class="{ disabled: currentPage() == 0 }">
            <a @click="currentPage() > 0 ? showPage(currentPage() - 1) : null" aria-label="Previous" :href="currentPage() > 0 ? 'javascript:void(0);' : null">
              <span aria-hidden="true">&laquo;</span>
            </a>
          </li>
          <li v-for="pageIndex in pagesToDisplay()" v-bind:class="{ active: currentPage() == pageIndex }">
            <a v-on:click="showPage(pageIndex)" href="javascript:void(0);">{{pageIndex + 1}}</a>
          </li>
          <li v-bind:class="{ disabled: currentPage() == maxPage() }">
            <a @click="currentPage() < maxPage() ? showPage(currentPage() + 1) : null" aria-label="Next" :href="currentPage() < maxPage() ? 'javascript:void(0);' : null">
              <span aria-hidden="true">&raquo;</span>
            </a>
          </li>
        </ul>
      </nav>
    </template>
</div>
`,
  data: function() {
    return {
      number_of_pages: 10,
    };
  },
  props: [],
  methods: {
    currentPage: function() {
      return this.$parent.page;
    },
    firstPage: function() {
      return Math.max(this.$parent.page - this.number_of_pages / 2, 0);
    },
    lastPage: function() {
      return Math.min(this.firstPage() + this.number_of_pages, this.maxPage());
    },
    maxPage: function() {
      return parseInt(this.$parent.count / this.$parent.page_size);
    },
    pagesToDisplay: function() {
      const result = [];
      for (let i = this.firstPage(); i <= this.lastPage(); i++) {
        result.push(i);
      }
      return result;
    },
    showPage: function(pageToShow) {
      this.$parent.page = pageToShow;
      this.$parent.loadTopics();
    },
    debug: function() {
      console.log('--- pagination');
      console.log('initial_page:' + this.$parent.initial_page);
      console.log('page:' + this.$parent.page);
      console.log('page_size:' + this.$parent.page_size);
      console.log('number_of_results:' + this.$parent.count);
      console.log('firstPage:' + this.firstPage());
      console.log('lastPage:' + this.lastPage());
      console.log('maxPage:' + this.maxPage());
      console.log('pagesToDisplay:' + this.pagesToDisplay());
    },
  },
  updated: function() {
    this.debug();
  },
  mounted: function() {
    this.debug();
  },
});
