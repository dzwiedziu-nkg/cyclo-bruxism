# -*- coding: utf-8 -*-
# Generated by Django 1.10.3 on 2017-02-26 17:40
from __future__ import unicode_literals

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('mydatabase', '0009_auto_20170117_1614'),
    ]

    operations = [
        migrations.AlterField(
            model_name='trip',
            name='trip_data',
            field=models.FileField(upload_to='tripData/'),
        ),
    ]
