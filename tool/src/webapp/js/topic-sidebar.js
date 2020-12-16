Vue.component('topic-sidebar', {
  template: `
<div :class="sidebarCss" v-if="initial_post">
    <ul>
        <li>
            <a href="javascript:void(0);" @click="toggleInfo()" :class="linkCSS('info')">
                <i class="fa fa-info-circle"></i>
                <span class="sr-only">Show Topic Info</span>
            </a>
        </li>
        <li v-if="isInstructor">
            <a href="javascript:void(0);" @click="toggleGrading()" :class="linkCSS('grading')" v-if="allowGrading">
                <i class="fa fa-star" style="color: green"></i>
                <span class="sr-only">Show Grading Info</span>
            </a>
        </li>
        <li v-if="isInstructor">
            <a href="javascript:void(0);" @click="toggleModeration()" :class="linkCSS('moderation')">
                <i class="fa fa-user" style="color: orange"></i>
                <span class="sr-only">Show Moderation Info</span>
            </a>
        </li>
    </ul>
    <div class="conversations-topic-sidebar-panel">
        <a href="javascript:void(0)" @click="toggle()" class="pull-right"><i class="fa fa-times"></i></a>
        <template v-if="panel === 'info'">
            <div class="conversations-topic-sidebar-panel-header">
                Topic Details
            </div>
            <div class="conversations-topic-sidebar-panel-notice">
                <div class="conversations-topic-sidebar-panel-notice-count">
                    {{reactionsCount}}
                </div>
                <div class="conversations-topic-sidebar-panel-notice-label">
                    Total Posts
                </div>
            </div>
            <div class="conversations-topic-sidebar-panel-content info">
                <div class="posters">
                  <div class="creator">
                    <div class="topic-poster-photo" :title="buildPosterTooltip(initialPoster)">
                      <img :src="buildPosterProfilePicSrc(initialPoster)" :alt="buildPosterProfilePicAlt(initialPoster)" />
                    </div>
                  </div>
                  <div v-for="poster in otherPosters" class="topic-poster-photo" :title="buildPosterTooltip(poster)">
                    <img :src="buildPosterProfilePicSrc(poster)" :alt="buildPosterProfilePicAlt(poster)" />
                  </div>
                </div>
                <div class="row">
                    <div class="col-sm-6 text-muted">Topic Type</div>
                    <div class="col-sm-6">React</div>
                </div>
                <div class="row">
                    <div class="col-sm-6 text-muted">Reactions</div>
                    <div class="col-sm-6">{{reactionsCount}}</div>
                </div>
                <div class="row" v-if="allowComments">
                    <div class="col-sm-6 text-muted">Comments</div>
                    <div class="col-sm-6">{{commentsCount}}</div>
                </div>
                <div class="row" v-if="allowLike">
                    <div class="col-sm-6 text-muted">Likes</div>
                    <div class="col-sm-6">{{likeCount}}</div>
                </div>
                <hr>
                <div class="row">
                    <div class="col-sm-6 text-muted">Created By</div>
                    <div class="col-sm-6">{{createdBy}}</div>
                </div>
                <div class="row">
                    <div class="col-sm-6 text-muted">Created On</div>
                    <div class="col-sm-6">{{createdAt}}</div>
                </div>
                <div class="row">
                    <div class="col-sm-6 text-muted">No. of Posters</div>
                    <div class="col-sm-6">{{noOfPosters}}</div>
                </div>
                <div class="row">
                    <div class="col-sm-6 text-muted">Most Recently by</div>
                    <div class="col-sm-6">{{mostRecentPoster}}</div>
                </div>
                <div class="row" v-if="allowGrading">
                    <div class="col-sm-6 text-muted">Avg. Grade</div>
                    <div class="col-sm-6">{{averageGrade}}</div>
                </div>
            </div>
        </template>
        <template v-else>
            <p style="font-style: italic;">Coming soon.</p>
        </template>
    </div>
</div>
`,
  data: function() {
    return {
        expanded: false,
        panel: undefined,
    };
  },
  props: ['current_user_role', 'topic', 'posts', 'initial_post'],
  computed: {
      sidebarCss: function() {
          if (this.expanded) {
              return 'conversations-topic-sidebar expanded';
          } else {
              return 'conversations-topic-sidebar collapsed';
          }
      },
      isInstructor: function() {
          return this.current_user_role === 'instructor';
      },
      reactionsCount: function() {
          return this.posts.length;
      },
      allowComments: function() {
          return this.topic.settings.allow_comments;
      },
      commentsCount: function() {
        var result = 0;

        this.posts.forEach((post) => {
          result += post.comments.length;
        });

        return result;
      },
      allowLike: function() {
          return this.topic.settings.allow_like;
      },
      likeCount: function() {
        var result = 0;

        this.posts.forEach((post) => {
          result += post.likes;
        });

        return result;
      },
      createdAt: function() {
          return this.$parent.formatEpochTime(this.topic.createdAt);
      },
      createdBy: function() {
          return this.initial_post.postedByDisplayName;
      },
      noOfPosters: function() {
          return this.topic.posters.length;
      },
      mostRecentPoster: function() {
          if (this.posts.length == 0) {
              return 'N/A';
          }
          return this.posts[this.posts.length - 1].postedByDisplayName;
      },
      allowGrading: function() {
          return this.topic.settings.allowGrading;
      },
      averageGrade: function() {
          return 'Coming soon.';
      },
      initialPoster: function() {
        var result = undefined;
        this.topic.posters.forEach((poster) => {
          if (poster.userId === this.initial_post.postedBy) {
            result = poster;
          }
        });
        return result;
      },
      otherPosters: function() {
        var result = [];

        this.topic.posters.forEach((poster) => {
          if (poster.userId != this.initial_post.postedBy) {
            result.push(poster);
          }
        });

        return result;
      }
  },
  methods: {
      toggle: function() {
          this.expanded = !this.expanded;
          if (!this.expanded) {
              this.panel = undefined;
          }
      },
      toggleInfo: function() {
          if (this.panel === undefined) {
            this.toggle();
            this.panel = 'info';
          } else if (this.panel === 'info') {
            this.toggle();
          } else {
              this.panel = 'info';
          }
      },
      toggleGrading: function() {
          if (this.panel === undefined) {
            this.toggle();
            this.panel = 'grading';
          } else if (this.panel === 'grading') {
            this.toggle();
          } else {
              this.panel = 'grading';
          }
      },
      toggleModeration: function() {
          if (this.panel === undefined) {
            this.toggle();
            this.panel = 'moderation';
          } else if (this.panel === 'moderation') {
            this.toggle();
          } else {
              this.panel = 'moderation';
          }
      },
      linkCSS: function(linkPanel) {
          if (this.expanded) {
              if (this.panel === linkPanel) {
                  return 'active';
              }
          }

          return '';
      },
      buildPosterTooltip: function(poster) {
          var posterName = poster.netId;
          if (poster.firstName) {
            posterName = poster.firstName + ' ' + poster.lastName;
          }
          return posterName + ' last posted at ' + this.$parent.formatEpochTime(poster.latestPostAt);
      },
      buildPosterProfilePicAlt(poster) {
        return 'Profile picture for ' + poster.netId;
      },
      buildPosterProfilePicSrc(poster) {
        return '/direct/profile/' + poster.userId + '/image';
      },
  },
  mounted: function() {
  },
});
