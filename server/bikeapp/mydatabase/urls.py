from django.conf.urls import url

from . import views

urlpatterns = [
	url(r'^$', views.index, name='index'),
	url(r'login/(?P<userName>[\w-]+)/(?P<password>[\w-]+)/$', views.login, name='login'),
	url(r'register/(?P<userName>[\w-]+)/(?P<password>[\w-]+)/$', views.register, name='register'),
	url(r'saveTrip/$', views.saveTrip, name='saveTrip'),
]