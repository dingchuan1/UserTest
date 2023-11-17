// 次级菜单栏和导航栏联动
var navItems = document.querySelectorAll('.navigation ul li');
var subMenus = document.querySelectorAll('.sidebar .sub-menu');

for (var i = 0; i < navItems.length; i++) {
  navItems[i].addEventListener('click', function() {
    for (var j = 0; j < navItems.length; j++) {
      navItems[j].classList.remove('active');
    }
    this.classList.add('active');

    var menuIndex = parseInt(this.getAttribute('data-index'));
    for (var k = 0; k < subMenus.length; k++) {
      subMenus[k].classList.remove('active');
    }
    subMenus[menuIndex].classList.add('active');
  });
}

// 菜单切换
var menuItems = document.querySelectorAll('.sidebar .menu-item');
var menuContents = document.querySelectorAll('.main .menu-content');

for (var i = 0; i < menuItems.length; i++) {
  menuItems[i].addEventListener('click', function() {
    for (var j = 0; j < menuItems.length; j++) {
      menuItems[j].classList.remove('active');
    }
    this.classList.add('active');

    var menuIndex = parseInt(this.getAttribute('data-index'));
    for (var k = 0; k < menuContents.length; k++) {
      menuContents[k].classList.remove('active');
    }
    menuContents[menuIndex].classList.add('active');
  });
}

// 响应式布局
window.addEventListener('resize', function() {
  // 根据屏幕宽度调整样式设计
});

