from django.shortcuts import render
from django.http import HttpResponse

import json

from .models import User

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

def saveTrip(request):
	if request.method == 'POST':
		json_data = json.loads(request.body)
		try:
			#Tu ma być odczytywanie wysłanych danych na temat podróży
			pass
		except KeyError:
			return HttpResponse('false')
		return HttpResponse('true')
	return HttpResponse('no_json')
