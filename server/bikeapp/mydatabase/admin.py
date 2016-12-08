from django.contrib import admin


from .models import User
from .models import Trip
from .models import TripDetail

admin.site.register(User)
admin.site.register(Trip)
admin.site.register(TripDetail)