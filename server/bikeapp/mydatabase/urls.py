from django.conf.urls import url

from . import views

urlpatterns = [
	url(r'^$', views.index, name='index'),
	url(r'login/(?P<userName>[\w-]+)/(?P<password>[\w-]+)/$', views.login, name='login'),
	url(r'register/(?P<userName>[\w-]+)/(?P<password>[\w-]+)/$', views.register, name='register'),
	url(r'saveTrip/(?P<userName>[\w-]+)/(?P<name>[\w-]+)/(?P<bikeType>[\w-]+)/(?P<phonePlacement>[\w-]+)/(?P<isPublic>[\w-]+)/$', views.saveTrip, name='saveTrip'),
	url(r'listTrip/(?P<userName>[\w-]+)/(?P<mode>[\w-]+)/$', views.listTrip, name='listTrip'),
	url(r'getTrip/(?P<id>[\w-]+)/$', views.getTrip, name='getTrip'),
	url(r'getRating/$', views.getRating, name='getRating'),
]