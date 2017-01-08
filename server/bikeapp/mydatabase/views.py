from django.shortcuts import render
from django.http import HttpResponse
from django.views.decorators.csrf import csrf_exempt

import json

from .models import User
from .models import Trip
from .forms import DocumentForm

def index(request):
	return HttpResponse("Hello world")

def login(request, userName, password):
	try:
		User.objects.get(user_name=userName, password=password)
	except User.DoesNotExist:
		return HttpResponse('false')
	return HttpResponse('true')

def register(request, userName, password):
	userCheck = User.objects.filter(user_name=userName)
	if userCheck:
		return HttpResponse('false')
	else:
		User.objects.create(user_name=userName, password=password)
		return HttpResponse('true')

@csrf_exempt 
def saveTrip(request, userName, name, bikeType, phonePlacement, isPublic):
	if request.method == 'POST':

		user = User.objects.filter(user_name=userName)
		form = DocumentForm(request.POST, request.FILES)

		if (user):# and form.is_valid()):
			boolIsPublic = False
			if isPublic == 'true':
				boolIsPublic = True


			Trip.objects.create(
				user_fkey=user.get(), 
				name=name, 
				bike_used=bikeType, 
				phone_placement=phonePlacement, 
				is_public=boolIsPublic, 
				data_file = request.FILES['docfile'])

			return HttpResponse('true')

		return HttpResponse('false')

	return HttpResponse('falsePost')
