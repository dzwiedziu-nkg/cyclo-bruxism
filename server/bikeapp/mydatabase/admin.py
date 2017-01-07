from django.contrib import admin


from .models import User
from .models import Trip

admin.site.register(User)
admin.site.register(Trip)